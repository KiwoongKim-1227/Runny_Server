package com.goodspace.runny.domain.quest.service;

import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.dog.entity.ChangeSource;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.service.DogExpService;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.domain.quest.dto.QuestDto;
import com.goodspace.runny.domain.quest.entity.Quest;
import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.entity.QuestType;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import com.goodspace.runny.domain.quest.repository.QuestRepository;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 퀘스트 서비스. 오늘의 퀘스트 조회(lazy 생성 + 접속 자동 달성), 보상 수령, 꾸미기 이벤트를 담당한다.
 * 보상 수령 시 경험치는 활성 강아지에게(DogExpService - QUEST_REWARD 변화 로그 자동 기록), 코인은 CoinService로 지급한다.
 */
@Service
@RequiredArgsConstructor
public class QuestService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DAILY_RANDOM_COUNT = 2;

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;
    private final QuestProgressService questProgressService;
    private final DogService dogService;
    private final DogExpService dogExpService;
    private final CoinService coinService;

    /** 오늘의 퀘스트 조회 - 없으면 lazy 생성(랜덤 2종 선택 + 수치 확정), 조회 시 접속하기 자동 달성 */
    @Transactional
    public QuestDto.TodayResponse getToday(Long userId) {
        String todayKey = QuestPeriod.todayKey();
        String weekKey = QuestPeriod.weekKey();

        List<UserQuest> daily = ensureDailyQuests(userId, todayKey);
        List<UserQuest> weekly = ensureWeeklyQuests(userId, weekKey);

        // 접속하기 퀘스트 자동 달성 (일일 첫 조회 시)
        daily.stream()
                .filter(uq -> uq.getQuest().getConditionType() == QuestConditionType.LOGIN)
                .forEach(UserQuest::completeNow);

        return new QuestDto.TodayResponse(
                daily.stream().map(QuestDto.QuestItem::from).toList(),
                weekly.stream().map(QuestDto.QuestItem::from).toList());
    }

    /** 보상 수령 - 달성 검증 후 경험치는 활성 강아지 addExp, 코인은 CoinService.add. 중복/만료 수령 방지 */
    @Transactional
    public QuestDto.ClaimResponse claim(Long userId, Long userQuestId) {
        UserQuest userQuest = userQuestRepository.findByIdAndUserId(userQuestId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUEST_004));

        // 만료 검증: 전일/전주 퀘스트는 수령 불가
        String currentKey = userQuest.getQuest().getType() == QuestType.WEEKLY
                ? QuestPeriod.weekKey() : QuestPeriod.todayKey();
        if (!currentKey.equals(userQuest.getPeriodKey())) {
            throw new BusinessException(ErrorCode.QUEST_003);
        }
        if (userQuest.isClaimed()) {
            throw new BusinessException(ErrorCode.QUEST_002);
        }
        if (!userQuest.isCompleted()) {
            throw new BusinessException(ErrorCode.QUEST_001);
        }

        userQuest.claim();

        // 경험치는 수령 시점의 활성 강아지에게 적립 (dog_change_log 자동 기록 포함)
        int rewardExp = userQuest.getQuest().getRewardExp();
        UserDog.ExpResult expResult = new UserDog.ExpResult(0, 0, 0);
        if (rewardExp > 0) {
            UserDog activeDog = dogService.getActiveDog(userId);
            expResult = dogExpService.addExp(activeDog, rewardExp, ChangeSource.QUEST_REWARD);
        }
        int rewardCoin = userQuest.getQuest().getRewardCoin();
        if (rewardCoin > 0) {
            coinService.add(userId, rewardCoin, CoinTransactionType.QUEST, userQuest.getId());
        }
        return new QuestDto.ClaimResponse(rewardExp, rewardCoin,
                expResult.levelBefore(), expResult.levelAfter(), expResult.leveledUp());
    }

    /** 히스토리 꾸미기 완료 이벤트 - 랜덤 퀘스트(DECORATE) 진행도 갱신 */
    @Transactional
    public void onDecorateEvent(Long userId) {
        questProgressService.onDecorated(userId);
    }

    /** 오늘 일일 퀘스트 확보 - 없으면 고정 3종 + 랜덤 풀에서 2종 선택 생성 */
    private List<UserQuest> ensureDailyQuests(Long userId, String todayKey) {
        List<UserQuest> existing = userQuestRepository.findByUserIdAndPeriodKey(userId, todayKey);
        if (!existing.isEmpty()) {
            return sortByQuestId(existing);
        }
        List<UserQuest> created = new ArrayList<>();
        for (Quest quest : questRepository.findByType(QuestType.DAILY_FIXED)) {
            created.add(new UserQuest(userId, quest, todayKey, quest.getMinValue()));
        }
        // 랜덤 풀 4종에서 2종 선택 + 수치형 조건은 범위 내 랜덤 값으로 확정(스냅샷)
        List<Quest> pool = new ArrayList<>(questRepository.findByType(QuestType.DAILY_RANDOM));
        Collections.shuffle(pool, RANDOM);
        pool.stream().limit(DAILY_RANDOM_COUNT).forEach(quest ->
                created.add(new UserQuest(userId, quest, todayKey, randomTarget(quest))));
        return saveIgnoringRace(created, userId, todayKey);
    }

    /** 이번 주 주간 퀘스트 확보 */
    private List<UserQuest> ensureWeeklyQuests(Long userId, String weekKey) {
        List<UserQuest> existing = userQuestRepository.findByUserIdAndPeriodKey(userId, weekKey);
        if (!existing.isEmpty()) {
            return sortByQuestId(existing);
        }
        List<UserQuest> created = questRepository.findByType(QuestType.WEEKLY).stream()
                .map(quest -> new UserQuest(userId, quest, weekKey, quest.getMinValue()))
                .toList();
        return saveIgnoringRace(created, userId, weekKey);
    }

    /** 동시 조회 경쟁 대응 - (user, quest, period) UNIQUE 위반 시 이미 생성된 데이터를 재조회 */
    private List<UserQuest> saveIgnoringRace(List<UserQuest> created, Long userId, String periodKey) {
        try {
            userQuestRepository.saveAll(created);
            userQuestRepository.flush();
            return sortByQuestId(created);
        } catch (DataIntegrityViolationException e) {
            return sortByQuestId(userQuestRepository.findByUserIdAndPeriodKey(userId, periodKey));
        }
    }

    /** 수치형 조건 랜덤 확정치 (정수 단위, min~max 포함) */
    private double randomTarget(Quest quest) {
        int min = (int) quest.getMinValue();
        int max = (int) quest.getMaxValue();
        return min >= max ? min : min + RANDOM.nextInt(max - min + 1);
    }

    private List<UserQuest> sortByQuestId(List<UserQuest> quests) {
        return quests.stream()
                .sorted(Comparator.comparing(uq -> uq.getQuest().getId()))
                .toList();
    }
}

package com.goodspace.runny.domain.quest.service;

import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.dog.entity.ChangeSource;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.service.DogExpService;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.domain.quest.dto.QuestDto;
import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.entity.QuestType;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 퀘스트 서비스. 오늘의 퀘스트 조회(lazy 출제 + 접속 자동 달성), 보상 수령, 꾸미기 이벤트를 담당한다.
 * 출제는 QuestAssignmentService에 위임하고, 보상 수령은 조건부 UPDATE로 중복 지급을 차단한다.
 */
@Service
@RequiredArgsConstructor
public class QuestService {

    private final UserQuestRepository userQuestRepository;
    private final QuestAssignmentService questAssignmentService;
    private final QuestProgressService questProgressService;
    private final DogService dogService;
    private final DogExpService dogExpService;
    private final CoinService coinService;

    /** 오늘의 퀘스트 조회 - 없으면 lazy 출제, 조회 시 접속하기 자동 달성 */
    @Transactional
    public QuestDto.TodayResponse getToday(Long userId) {
        List<UserQuest> daily = questAssignmentService.ensureDailyQuests(userId, QuestPeriod.todayKey());
        List<UserQuest> weekly = questAssignmentService.ensureWeeklyQuests(userId, QuestPeriod.weekKey());

        // 접속하기 퀘스트 자동 달성 (일일 첫 조회 시)
        daily.stream()
                .filter(uq -> uq.getQuest().getConditionType() == QuestConditionType.LOGIN)
                .forEach(UserQuest::completeNow);

        return new QuestDto.TodayResponse(
                daily.stream().map(QuestDto.QuestItem::from).toList(),
                weekly.stream().map(QuestDto.QuestItem::from).toList());
    }

    /**
     * 보상 수령 - 달성 검증 후 조건부 UPDATE(claimed=false -> true)로 수령을 선점한 요청만 보상을 지급한다.
     * 더블클릭/동시 요청이 와도 영향 행이 1인 요청 하나만 지급되므로 중복 지급이 원천 차단된다.
     */
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
        if (!userQuest.isCompleted()) {
            throw new BusinessException(ErrorCode.QUEST_001);
        }
        // 보상 정보는 벌크 UPDATE 전에 미리 확보 (영속성 컨텍스트와 무관한 로컬 값)
        int rewardExp = userQuest.getQuest().getRewardExp();
        int rewardCoin = userQuest.getQuest().getRewardCoin();

        // 수령 선점 - 이미 수령됐거나 동시 요청에 밀리면 영향 행 0
        if (userQuestRepository.claimIfNotClaimed(userQuestId, userId) == 0) {
            throw new BusinessException(ErrorCode.QUEST_002);
        }

        // 경험치는 수령 시점의 활성 강아지에게 적립 (dog_change_log 자동 기록 포함)
        UserDog.ExpResult expResult = new UserDog.ExpResult(0, 0, 0);
        if (rewardExp > 0) {
            UserDog activeDog = dogService.getActiveDog(userId);
            expResult = dogExpService.addExp(activeDog, rewardExp, ChangeSource.QUEST_REWARD);
        }
        if (rewardCoin > 0) {
            coinService.add(userId, rewardCoin, CoinTransactionType.QUEST, userQuestId);
        }
        return new QuestDto.ClaimResponse(rewardExp, rewardCoin,
                expResult.levelBefore(), expResult.levelAfter(), expResult.leveledUp());
    }

    /** 히스토리 꾸미기 완료 이벤트 - 랜덤 퀘스트(DECORATE) 진행도 갱신 */
    @Transactional
    public void onDecorateEvent(Long userId) {
        questProgressService.onDecorated(userId);
    }
}

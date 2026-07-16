package com.goodspace.runny.domain.quest.service;

import com.goodspace.runny.domain.quest.entity.Quest;
import com.goodspace.runny.domain.quest.entity.QuestType;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import com.goodspace.runny.domain.quest.repository.QuestRepository;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
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
 * 퀘스트 lazy 출제 공용 서비스. 조회(QuestService)와 진행도 갱신(QuestProgressService)이 모두 사용한다.
 * 오늘의 퀘스트를 조회하지 않고 러닝을 완료해도 이 서비스가 먼저 출제해 진행도가 유실되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class QuestAssignmentService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DAILY_RANDOM_COUNT = 2;

    private final QuestRepository questRepository;
    private final UserQuestRepository userQuestRepository;

    /** 오늘 일일 퀘스트 확보 - 없으면 고정 3종 + 랜덤 풀에서 2종 선택 생성(수치 확정 스냅샷) */
    @Transactional
    public List<UserQuest> ensureDailyQuests(Long userId, String todayKey) {
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
    @Transactional
    public List<UserQuest> ensureWeeklyQuests(Long userId, String weekKey) {
        List<UserQuest> existing = userQuestRepository.findByUserIdAndPeriodKey(userId, weekKey);
        if (!existing.isEmpty()) {
            return sortByQuestId(existing);
        }
        List<UserQuest> created = questRepository.findByType(QuestType.WEEKLY).stream()
                .map(quest -> new UserQuest(userId, quest, weekKey, quest.getMinValue()))
                .toList();
        return saveIgnoringRace(created, userId, weekKey);
    }

    /** 동시 요청 경쟁 대응 - (user, quest, period) UNIQUE 위반 시 이미 생성된 데이터를 재조회 */
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

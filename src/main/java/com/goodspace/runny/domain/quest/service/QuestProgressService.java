package com.goodspace.runny.domain.quest.service;

import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀘스트 진행도 갱신 공용 서비스. 러닝 완료(러닝 도메인에서 호출)와 히스토리 꾸미기 이벤트를 받아
 * 현재 기간(오늘/이번 주)의 해당 조건 퀘스트들을 갱신한다.
 * 갱신 전 QuestAssignmentService로 오늘/이번 주 퀘스트를 먼저 출제해,
 * 퀘스트 화면을 열지 않고 러닝한 경우에도 진행도가 유실되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class QuestProgressService {

    private final UserQuestRepository userQuestRepository;
    private final QuestAssignmentService questAssignmentService;

    /** 러닝 완료 이벤트 - 러닝 도메인(9단계)에서 호출할 공개 메서드 */
    public record RunningEvent(
            double distanceKm,
            long durationSec,
            long longestNonstopSec
    ) {
    }

    /**
     * 러닝 완료 시 진행도 일괄 갱신.
     * 1회 기록형(거리/시간/무정지)은 최대치 갱신, 횟수형(오늘의 러닝/주 3회)과 주간 누적 거리는 누적 가산.
     */
    @Transactional
    public void onRunningCompleted(Long userId, RunningEvent event) {
        String todayKey = QuestPeriod.todayKey();
        String weekKey = QuestPeriod.weekKey();
        // 미출제 기간이면 먼저 출제해 진행도 유실 방지
        questAssignmentService.ensureDailyQuests(userId, todayKey);
        questAssignmentService.ensureWeeklyQuests(userId, weekKey);

        // 1회 러닝 거리 (1km 달리기 / 랜덤 3~6km)
        updateMax(userId, todayKey, QuestConditionType.DISTANCE, event.distanceKm());
        // 1회 러닝 시간 - 분 단위 (랜덤 30~60분)
        updateMax(userId, todayKey, QuestConditionType.DURATION, event.durationSec() / 60.0);
        // 1회 최장 무정지 시간 - 분 단위 (랜덤 15~30분)
        updateMax(userId, todayKey, QuestConditionType.NONSTOP, event.longestNonstopSec() / 60.0);
        // 러닝 횟수 - 오늘의 러닝 완료(일일) + 주 3회 러닝(주간)
        addProgress(userId, todayKey, QuestConditionType.RUN_COUNT, 1);
        addProgress(userId, weekKey, QuestConditionType.RUN_COUNT, 1);
        // 주간 누적 거리 (누적 10km)
        addProgress(userId, weekKey, QuestConditionType.WEEK_DISTANCE, event.distanceKm());
    }

    /** 히스토리 꾸미기 완료 이벤트 - 랜덤 퀘스트(DECORATE) 진행도 */
    @Transactional
    public void onDecorated(Long userId) {
        String todayKey = QuestPeriod.todayKey();
        questAssignmentService.ensureDailyQuests(userId, todayKey);
        addProgress(userId, todayKey, QuestConditionType.DECORATE, 1);
    }

    /** 최대치 갱신 방식 */
    private void updateMax(Long userId, String periodKey, QuestConditionType conditionType, double value) {
        userQuestRepository.findForProgress(userId, periodKey, conditionType)
                .forEach(userQuest -> userQuest.updateMaxProgress(value));
    }

    /** 누적 가산 방식 */
    private void addProgress(Long userId, String periodKey, QuestConditionType conditionType, double delta) {
        userQuestRepository.findForProgress(userId, periodKey, conditionType)
                .forEach(userQuest -> userQuest.addProgress(delta));
    }
}

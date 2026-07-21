package com.goodspace.runny.domain.quest.service;

import com.goodspace.runny.domain.achievement.service.AchievementService;
import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 퀘스트 진행도 갱신 공용 서비스. 접속/러닝 완료/꾸미기 이벤트를 받아
 * 현재 기간(오늘/이번 주)의 해당 조건 퀘스트들을 갱신한다.
 * 갱신 전 QuestAssignmentService로 오늘/이번 주 퀘스트를 먼저 출제해 진행도 유실을 방지하고,
 * 갱신 후에는 퀘스트 완료 연동 업적(총 완료 100개, 일일 올클리어) 판정 훅을 호출한다.
 */
@Service
@RequiredArgsConstructor
public class QuestProgressService {

    private final UserQuestRepository userQuestRepository;
    private final QuestAssignmentService questAssignmentService;
    private final AchievementService achievementService;

    /** 러닝 완료 이벤트 - 러닝 도메인에서 호출. newRoute/steadyPaceKm은 프론트 판정/계산 값 */
    public record RunningEvent(
            double distanceKm,
            long durationSec,
            long longestNonstopSec,
            int calories,
            Boolean newRoute,
            Double steadyPaceKm,
            LocalDateTime endedAt
    ) {
    }

    /**
     * 접속 이벤트 - 오늘의 퀘스트 조회 시 호출. 접속하기 퀘스트 자동 달성 +
     * 오늘 첫 접속이면 주간 접속 일수(WEEK_LOGIN_DAYS) 1일 가산 (하루 1회만).
     */
    @Transactional
    public void onLogin(Long userId) {
        String todayKey = QuestPeriod.todayKey();
        String weekKey = QuestPeriod.weekKey();
        questAssignmentService.ensureDailyQuests(userId, todayKey);
        questAssignmentService.ensureWeeklyQuests(userId, weekKey);

        List<UserQuest> loginQuests = userQuestRepository.findForProgress(userId, todayKey, QuestConditionType.LOGIN);
        boolean firstLoginToday = loginQuests.stream().anyMatch(uq -> !uq.isCompleted());
        loginQuests.forEach(UserQuest::completeNow);
        if (firstLoginToday) {
            addProgress(userId, weekKey, QuestConditionType.WEEK_LOGIN_DAYS, 1);
        }
        achievementService.onQuestUpdated(userId, todayKey);
    }

    /**
     * 러닝 완료 시 진행도 일괄 갱신.
     * 1회 기록형(거리/시간/무정지/일정 페이스)은 최대치 갱신, 횟수/누적형은 가산.
     * 무정지(NONSTOP)는 일일(15분)과 주간(30분) 퀘스트가 공유하므로 두 기간 모두 갱신한다.
     */
    @Transactional
    public void onRunningCompleted(Long userId, RunningEvent event) {
        String todayKey = QuestPeriod.todayKey();
        String weekKey = QuestPeriod.weekKey();
        // 미출제 기간이면 먼저 출제해 진행도 유실 방지
        questAssignmentService.ensureDailyQuests(userId, todayKey);
        questAssignmentService.ensureWeeklyQuests(userId, weekKey);

        double durationMin = event.durationSec() / 60.0;
        double nonstopMin = event.longestNonstopSec() / 60.0;

        // 일일 - 1회 러닝 거리 (1km 이상 / 3km)
        updateMax(userId, todayKey, QuestConditionType.DISTANCE, event.distanceKm());
        // 일일 - 1회 러닝 시간 (30분 이상)
        updateMax(userId, todayKey, QuestConditionType.DURATION, durationMin);
        // 무정지 - 일일 15분 / 주간 30분 공용
        updateMax(userId, todayKey, QuestConditionType.NONSTOP, nonstopMin);
        updateMax(userId, weekKey, QuestConditionType.NONSTOP, nonstopMin);
        // 일일 - 일정 페이스 유지 거리 (프론트 계산 값, 미전달 시 0)
        updateMax(userId, todayKey, QuestConditionType.STEADY_PACE,
                event.steadyPaceKm() != null ? event.steadyPaceKm() : 0);
        // 일일 - 새로운 경로로 1km 이상 (신규 경로 여부는 프론트 판정)
        if (Boolean.TRUE.equals(event.newRoute()) && event.distanceKm() >= 1) {
            addProgress(userId, todayKey, QuestConditionType.NEW_ROUTE, 1);
        }
        // 일일 - 오늘의 러닝 완료하기 (횟수)
        addProgress(userId, todayKey, QuestConditionType.RUN_COUNT, 1);
        // 주간 - 1km 이상 러닝 완료 횟수 (주 3회)
        if (event.distanceKm() >= 1) {
            addProgress(userId, weekKey, QuestConditionType.WEEK_RUN_1KM, 1);
        }
        // 주간 - 누적 거리 (15km) / 지난주 초과(누적 거리 동일 가산, target만 다름)
        addProgress(userId, weekKey, QuestConditionType.WEEK_DISTANCE, event.distanceKm());
        addProgress(userId, weekKey, QuestConditionType.LAST_WEEK_BEAT, event.distanceKm());
        // 주간 - 누적 시간 (100분)
        addProgress(userId, weekKey, QuestConditionType.WEEK_DURATION, durationMin);
        // 주간 - 누적 칼로리 (700kcal)
        addProgress(userId, weekKey, QuestConditionType.WEEK_CALORIES, event.calories());
        // 주간 - 주말(토/일, 종료 시각 기준) 누적 거리 (7km)
        DayOfWeek day = event.endedAt().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            addProgress(userId, weekKey, QuestConditionType.WEEKEND_DISTANCE, event.distanceKm());
        }
        achievementService.onQuestUpdated(userId, todayKey);
    }

    /** 히스토리 꾸미기 완료 이벤트 - 랜덤 퀘스트(DECORATE) 진행도 + 사진 작가 업적 카운트 */
    @Transactional
    public void onDecorated(Long userId) {
        String todayKey = QuestPeriod.todayKey();
        questAssignmentService.ensureDailyQuests(userId, todayKey);
        addProgress(userId, todayKey, QuestConditionType.DECORATE, 1);
        achievementService.onDecorated(userId);
        achievementService.onQuestUpdated(userId, todayKey);
    }

    /** 히스토리 꾸미기 창 접속 이벤트 - 랜덤 퀘스트(DECORATE_ENTER) 진행도 */
    @Transactional
    public void onDecorateEntered(Long userId) {
        String todayKey = QuestPeriod.todayKey();
        questAssignmentService.ensureDailyQuests(userId, todayKey);
        addProgress(userId, todayKey, QuestConditionType.DECORATE_ENTER, 1);
        achievementService.onQuestUpdated(userId, todayKey);
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

package com.goodspace.runny.domain.crew.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 크루 주간 top3 거리 집계 제공자. running_record는 9단계 구현이므로
 * 지금은 빈 목록을 반환하는 기본 구현을 두고 9단계에서 실제 집계 구현으로 교체한다.
 * 주간 기준: 이번 주 월요일 00:00 (KST) 이후의 러닝 기록 거리 합산 상위 3명.
 */
public interface CrewWeeklyDistanceProvider {

    /** 주간 거리 합산 결과 (유저 ID + 거리 km) */
    record MemberDistance(Long userId, double distanceKm) {
    }

    /** 이번 주(월요일 00:00 KST 기준) 크루원 러닝 거리 상위 3명 */
    List<MemberDistance> weeklyTop3(Long crewId);

    /** 이번 주 시작 시각 (월요일 00:00 KST) 계산 공통 유틸 */
    static LocalDateTime weekStart() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay();
    }

    /**
     * 기본 구현 - 9단계에서 아래 집계 쿼리 기반 실제 구현으로 교체한다.
     * 예정 쿼리 (RunningRecordRepository):
     *   SELECT r.userId, SUM(r.distanceKm) FROM RunningRecord r
     *   WHERE r.userId IN (SELECT m.userId FROM CrewMember m WHERE m.crewId = :crewId)
     *     AND r.endedAt >= :weekStart
     *   GROUP BY r.userId ORDER BY SUM(r.distanceKm) DESC LIMIT 3
     */
    @Component
    class NotYetImplemented implements CrewWeeklyDistanceProvider {
        @Override
        public List<MemberDistance> weeklyTop3(Long crewId) {
            // TODO(9단계): running_record 집계 구현으로 교체
            return List.of();
        }
    }
}

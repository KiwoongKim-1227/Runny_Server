package com.goodspace.runny.domain.crew.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 크루 주간 top3 거리 집계 제공자. 실제 구현은 러닝 도메인의 CrewWeeklyDistanceProviderImpl(9단계).
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
}

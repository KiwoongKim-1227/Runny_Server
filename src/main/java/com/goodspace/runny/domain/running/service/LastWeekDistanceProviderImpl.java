package com.goodspace.runny.domain.running.service;

import com.goodspace.runny.domain.quest.service.LastWeekDistanceProvider;
import com.goodspace.runny.domain.running.repository.RunningRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 지난주 총 러닝 거리 실제 구현 (퀘스트 도메인 인터페이스 교체).
 * "지난주 총 러닝 거리보다 많이 달리기" 주간 퀘스트의 출제 시점 target 계산에 사용한다.
 */
@Component
@RequiredArgsConstructor
public class LastWeekDistanceProviderImpl implements LastWeekDistanceProvider {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final RunningRecordRepository runningRecordRepository;

    /** 지난주 월요일 00:00 ~ 이번 주 월요일 00:00 (KST) 러닝 거리 합계 */
    @Override
    public double lastWeekDistanceKm(Long userId) {
        LocalDate thisMonday = LocalDate.now(ZONE_SEOUL).with(DayOfWeek.MONDAY);
        LocalDateTime start = thisMonday.minusWeeks(1).atStartOfDay();
        LocalDateTime end = thisMonday.atStartOfDay();
        return runningRecordRepository.sumDistanceBetween(userId, start, end);
    }
}

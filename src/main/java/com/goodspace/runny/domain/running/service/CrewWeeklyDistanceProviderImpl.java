package com.goodspace.runny.domain.running.service;

import com.goodspace.runny.domain.crew.service.CrewWeeklyDistanceProvider;
import com.goodspace.runny.domain.running.repository.RunningRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 크루 주간 top3 실제 구현 (7단계 인터페이스 교체). running_record를 크루원 기준으로
 * 이번 주 월요일 00:00 (KST) 이후 거리 합산해 상위 3명을 반환한다.
 */
@Component
@RequiredArgsConstructor
public class CrewWeeklyDistanceProviderImpl implements CrewWeeklyDistanceProvider {

    private final RunningRecordRepository runningRecordRepository;

    @Override
    public List<MemberDistance> weeklyTop3(Long crewId) {
        return runningRecordRepository
                .sumWeeklyDistanceByCrew(crewId, CrewWeeklyDistanceProvider.weekStart(), PageRequest.of(0, 3))
                .stream()
                .map(row -> new MemberDistance((Long) row[0], ((Number) row[1]).doubleValue()))
                .toList();
    }
}

package com.goodspace.runny.domain.running.repository;

import com.goodspace.runny.domain.running.entity.RunningRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 러닝 기록 리포지토리. 멱등키 조회, 월별 히스토리, 어제 평균 페이스, 크루 주간 집계를 제공한다.
 */
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {

    Optional<RunningRecord> findByClientRunId(String clientRunId);

    Optional<RunningRecord> findByIdAndUserId(Long id, Long userId);

    /** 월별 기록 목록 (최신순) */
    List<RunningRecord> findByUserIdAndEndedAtBetweenOrderByEndedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    /** 미확인 리포트 존재 여부 (홈 빨간 점) */
    boolean existsByUserIdAndCheckedFalse(Long userId);

    /** 어제 평균 페이스 (기록 없으면 null) - 넘치는 에너지 방출 업적 판정용 */
    @Query("SELECT AVG(r.avgPaceSec) FROM RunningRecord r " +
            "WHERE r.userId = :userId AND r.endedAt >= :start AND r.endedAt < :end")
    Double avgPaceBetween(@Param("userId") Long userId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /** 크루 주간 거리 상위 집계 (7단계 CrewWeeklyDistanceProvider 실제 구현용) */
    @Query("SELECT r.userId, SUM(r.distanceKm) FROM RunningRecord r " +
            "WHERE r.userId IN (SELECT m.userId FROM com.goodspace.runny.domain.crew.entity.CrewMember m " +
            "                   WHERE m.crewId = :crewId) " +
            "AND r.endedAt >= :weekStart " +
            "GROUP BY r.userId ORDER BY SUM(r.distanceKm) DESC")
    List<Object[]> sumWeeklyDistanceByCrew(@Param("crewId") Long crewId,
                                           @Param("weekStart") LocalDateTime weekStart,
                                           Pageable pageable);
}

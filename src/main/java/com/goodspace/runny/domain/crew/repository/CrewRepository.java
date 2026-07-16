package com.goodspace.runny.domain.crew.repository;

import com.goodspace.runny.domain.crew.entity.Crew;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 크루 리포지토리. 정원 검증이 필요한 흐름(가입 신청/일괄 승인/정원 확장)은
 * 비관적 쓰기 락으로 크루 행을 잠가 동시 요청 간 정원 초과를 방지한다.
 */
public interface CrewRepository extends JpaRepository<Crew, Long> {

    boolean existsByName(String name);

    Optional<Crew> findByName(String name);

    /** 크루명 부분 일치 검색 (페이징 제거 - 전체 반환) */
    List<Crew> findByNameContainingOrderByIdAsc(String name);

    /** 비관적 쓰기 락 조회 - 정원 검증/변경 흐름의 동시성 제어용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Crew c WHERE c.id = :crewId")
    Optional<Crew> findByIdForUpdate(@Param("crewId") Long crewId);
}

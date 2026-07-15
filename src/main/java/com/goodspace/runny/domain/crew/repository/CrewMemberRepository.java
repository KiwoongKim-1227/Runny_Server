package com.goodspace.runny.domain.crew.repository;

import com.goodspace.runny.domain.crew.entity.CrewMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 크루 멤버십 리포지토리. user_id UNIQUE로 1인 1크루.
 */
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    Optional<CrewMember> findByUserId(Long userId);

    Optional<CrewMember> findByCrewIdAndUserId(Long crewId, Long userId);

    boolean existsByUserId(Long userId);

    List<CrewMember> findByCrewIdOrderByJoinedAtAsc(Long crewId);

    int countByCrewId(Long crewId);

    /** 검색 결과 크루들의 현재 인원 일괄 집계 (crewId, count) */
    @Query("SELECT m.crewId, COUNT(m) FROM CrewMember m WHERE m.crewId IN :crewIds GROUP BY m.crewId")
    List<Object[]> countByCrewIds(@Param("crewIds") List<Long> crewIds);

    /** 크루 해체 시 멤버십 전부 삭제 */
    @Modifying
    @Query("DELETE FROM CrewMember m WHERE m.crewId = :crewId")
    int deleteAllByCrewId(@Param("crewId") Long crewId);
}

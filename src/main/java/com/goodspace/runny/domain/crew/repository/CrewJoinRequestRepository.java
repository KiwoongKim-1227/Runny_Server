package com.goodspace.runny.domain.crew.repository;

import com.goodspace.runny.domain.crew.entity.CrewJoinRequest;
import com.goodspace.runny.domain.crew.entity.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 크루 가입 신청 리포지토리. PENDING 상태 기준으로 조회하며 처리된 요청은 목록에서 제외된다.
 */
public interface CrewJoinRequestRepository extends JpaRepository<CrewJoinRequest, Long> {

    boolean existsByCrewIdAndUserIdAndStatus(Long crewId, Long userId, JoinRequestStatus status);

    Optional<CrewJoinRequest> findByCrewIdAndUserIdAndStatus(Long crewId, Long userId, JoinRequestStatus status);

    List<CrewJoinRequest> findByCrewIdAndStatusOrderByIdAsc(Long crewId, JoinRequestStatus status);

    int countByCrewIdAndStatus(Long crewId, JoinRequestStatus status);

    /** 내가 PENDING 신청한 크루 ID 목록 (검색 응답 myRequestStatus 분기용) */
    @Query("SELECT r.crewId FROM CrewJoinRequest r WHERE r.userId = :userId AND r.status = 'PENDING'")
    List<Long> findPendingCrewIdsOf(@Param("userId") Long userId);

    /** 크루 해체 시 신청 전부 삭제 */
    @Modifying
    @Query("DELETE FROM CrewJoinRequest r WHERE r.crewId = :crewId")
    int deleteAllByCrewId(@Param("crewId") Long crewId);

    /** 회원 탈퇴 시 본인 신청 전부 삭제 */
    @Modifying
    @Query("DELETE FROM CrewJoinRequest r WHERE r.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}

package com.goodspace.runny.domain.friend.repository;

import com.goodspace.runny.domain.friend.entity.Friendship;
import com.goodspace.runny.domain.friend.entity.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 친구 관계 리포지토리. 관계 조회는 정규화 컬럼(user_low_id/user_high_id) 기준으로 방향과 무관하게 찾는다.
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** 두 유저 사이의 관계 조회 (방향 무관) */
    @Query("SELECT f FROM Friendship f WHERE f.userLowId = :lowId AND f.userHighId = :highId")
    Optional<Friendship> findBetween(@Param("lowId") Long lowId, @Param("highId") Long highId);

    /** 나와 관련된 모든 관계 조회 (검색 결과 관계 상태 매핑용) */
    @Query("SELECT f FROM Friendship f WHERE f.requesterId = :userId OR f.receiverId = :userId")
    List<Friendship> findAllInvolving(@Param("userId") Long userId);

    /** 내 친구 목록 (ACCEPTED, 방향 무관) */
    @Query("SELECT f FROM Friendship f WHERE f.status = 'ACCEPTED' " +
            "AND (f.requesterId = :userId OR f.receiverId = :userId)")
    List<Friendship> findAcceptedOf(@Param("userId") Long userId);

    /** 보낸 요청 목록 (PENDING) */
    List<Friendship> findByRequesterIdAndStatusOrderByIdDesc(Long requesterId, FriendshipStatus status);

    /** 받은 요청 목록 (PENDING) */
    List<Friendship> findByReceiverIdAndStatusOrderByIdDesc(Long receiverId, FriendshipStatus status);

    /** 받은 요청 중 미확인 존재 여부 (빨간 점) */
    boolean existsByReceiverIdAndStatusAndCheckedFalse(Long receiverId, FriendshipStatus status);

    /** 받은 요청 일괄 확인 처리 (목록 조회 시점) */
    @Modifying
    @Query("UPDATE Friendship f SET f.checked = true " +
            "WHERE f.receiverId = :receiverId AND f.status = 'PENDING' AND f.checked = false")
    int markReceivedChecked(@Param("receiverId") Long receiverId);

    /** 유저와 관련된 모든 관계 삭제 (회원 탈퇴 시) */
    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.requesterId = :userId OR f.receiverId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}

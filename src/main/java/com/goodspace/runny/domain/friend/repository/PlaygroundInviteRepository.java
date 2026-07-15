package com.goodspace.runny.domain.friend.repository;

import com.goodspace.runny.domain.friend.entity.PlaygroundInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 놀이터 초대 리포지토리. 전체 교체(PUT) 저장과 친구 삭제/탈퇴 시 정리를 지원한다.
 */
public interface PlaygroundInviteRepository extends JpaRepository<PlaygroundInvite, Long> {

    List<PlaygroundInvite> findByOwnerIdOrderByIdAsc(Long ownerId);

    void deleteByOwnerId(Long ownerId);

    /** 두 유저 사이의 초대 상호 제거 (친구 삭제 시) */
    @Modifying
    @Query("DELETE FROM PlaygroundInvite p WHERE " +
            "(p.ownerId = :userA AND p.friendUserId = :userB) OR (p.ownerId = :userB AND p.friendUserId = :userA)")
    int deleteBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    /** 유저와 관련된 모든 초대 삭제 (회원 탈퇴 시) */
    @Modifying
    @Query("DELETE FROM PlaygroundInvite p WHERE p.ownerId = :userId OR p.friendUserId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}

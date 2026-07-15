package com.goodspace.runny.domain.friend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 친구 관계 엔티티. 요청(PENDING) -> 수락(ACCEPTED)의 단방향 요청/양방향 관계 모델.
 * 정규화 컬럼 user_low_id/user_high_id(두 유저 ID의 LEAST/GREATEST)에 복합 UNIQUE를 걸어
 * A->B와 B->A의 동시 요청을 DB 레벨에서 차단한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friendship", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"user_low_id", "user_high_id"})
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    // 두 유저 ID의 LEAST/GREATEST 정규화 값 - 양방향 중복 방지 UNIQUE 대상
    @Column(name = "user_low_id", nullable = false)
    private Long userLowId;

    @Column(name = "user_high_id", nullable = false)
    private Long userHighId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FriendshipStatus status;

    // 받은 요청의 확인 여부 (받은 요청 목록 빨간 점 판정용)
    @Column(name = "is_checked", nullable = false)
    private boolean checked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Friendship(Long requesterId, Long receiverId) {
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.userLowId = Math.min(requesterId, receiverId);
        this.userHighId = Math.max(requesterId, receiverId);
        this.status = FriendshipStatus.PENDING;
        this.checked = false;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /** 요청 수락 - 양방향 친구 관계로 전이 */
    public void accept() {
        this.status = FriendshipStatus.ACCEPTED;
    }

    /** 상대 유저 ID 반환 (내 ID 기준) */
    public Long otherUserId(Long myUserId) {
        return this.requesterId.equals(myUserId) ? this.receiverId : this.requesterId;
    }
}

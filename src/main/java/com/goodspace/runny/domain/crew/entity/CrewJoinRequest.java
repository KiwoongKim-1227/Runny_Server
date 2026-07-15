package com.goodspace.runny.domain.crew.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 크루 가입 신청 엔티티. 크루장이 승인/거절하며 일괄 처리를 지원한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "crew_join_request")
public class CrewJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private JoinRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CrewJoinRequest(Long crewId, Long userId) {
        this.crewId = crewId;
        this.userId = userId;
        this.status = JoinRequestStatus.PENDING;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /** 승인 처리 */
    public void approve() {
        this.status = JoinRequestStatus.APPROVED;
    }

    /** 거절 처리 */
    public void reject() {
        this.status = JoinRequestStatus.REJECTED;
    }
}

package com.goodspace.runny.domain.crew.entity;

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
 * 크루 멤버십 엔티티. user_id UNIQUE로 1인 1크루를 DB 레벨에서 보장한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "crew_member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crew_member_user", columnNames = "user_id")
})
public class CrewMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "crew_id", nullable = false)
    private Long crewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CrewRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    public CrewMember(Long crewId, Long userId, CrewRole role) {
        this.crewId = crewId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /** 역할 변경 (크루장 위임 시) */
    public void changeRole(CrewRole role) {
        this.role = role;
    }
}

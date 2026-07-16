package com.goodspace.runny.domain.achievement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 유저 업적 달성 엔티티. 업적은 1회성이며 달성 기록과 보상 수령(claimed)을 분리한다(수령 버튼 방식).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_achievement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_achievement", columnNames = {"user_id", "achievement_id"})
})
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "achieved_at", nullable = false, updatable = false)
    private LocalDateTime achievedAt;

    @Column(nullable = false)
    private boolean claimed;

    public UserAchievement(Long userId, Achievement achievement) {
        this.userId = userId;
        this.achievement = achievement;
        this.achievedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        this.claimed = false;
    }

    /** 보상 수령 처리 */
    public void claim() {
        this.claimed = true;
    }
}

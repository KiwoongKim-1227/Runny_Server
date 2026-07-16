package com.goodspace.runny.domain.achievement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 랜드마크 방문 기록 엔티티. (유저+랜드마크) UNIQUE로 최초 방문만 기록한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_landmark_visit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_landmark", columnNames = {"user_id", "landmark_id"})
})
public class UserLandmarkVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "landmark_id", nullable = false)
    private Long landmarkId;

    @Column(name = "visited_at", nullable = false, updatable = false)
    private LocalDateTime visitedAt;

    public UserLandmarkVisit(Long userId, Long landmarkId) {
        this.userId = userId;
        this.landmarkId = landmarkId;
        this.visitedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}

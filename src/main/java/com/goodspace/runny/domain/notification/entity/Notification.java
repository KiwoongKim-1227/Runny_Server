package com.goodspace.runny.domain.notification.entity;

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
 * 알림 엔티티. type(enum) + payload(JSON 문자열 - 타입별 참조 데이터) 확장형 구조이며
 * 표시 메시지는 저장하지 않고 조회 시 타입별로 조립한다. 푸시(FCM) 발송은 MVP 범위 외.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // 타입별 참조 데이터 JSON (예: FRIEND_REQUEST -> {"requesterId":1,"requesterNickname":"러니"})
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification(Long userId, NotificationType type, String payload) {
        this.userId = userId;
        this.type = type;
        this.payload = payload;
        this.read = false;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}

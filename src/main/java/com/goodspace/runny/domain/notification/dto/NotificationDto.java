package com.goodspace.runny.domain.notification.dto;

import com.goodspace.runny.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

/**
 * 알림 응답 DTO 모음.
 */
public final class NotificationDto {

    private NotificationDto() {
    }

    /** 알림 항목 - message는 조회 시 타입별로 조립된 표시 문구 */
    public record Item(
            Long id,
            NotificationType type,
            String message,
            boolean wasRead,
            LocalDateTime createdAt
    ) {
    }

    /** 미읽음 존재 여부 응답 (빨간 점) */
    public record UnreadExists(
            boolean exists
    ) {
    }
}

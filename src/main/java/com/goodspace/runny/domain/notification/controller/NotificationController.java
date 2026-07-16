package com.goodspace.runny.domain.notification.controller;

import com.goodspace.runny.domain.notification.dto.NotificationDto;
import com.goodspace.runny.domain.notification.service.NotificationService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 알림 API 컨트롤러. 목록 조회(읽음 처리)와 미읽음 존재 여부를 제공한다.
 */
@Tag(name = "Notification", description = "알림 API - 목록(조회 시 읽음 처리), 미읽음 빨간 점")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 알림 목록 조회 */
    @Operation(summary = "알림 목록 조회",
            description = "타입별 조립된 표시 메시지 반환. 조회 시점에 전체 읽음 처리(wasRead는 조회 시점 이전의 읽음 여부)")
    @GetMapping
    public ApiResponse<List<NotificationDto.Item>> getNotifications() {
        return ApiResponse.ok(notificationService.getNotifications(SecurityUtil.currentUserId()));
    }

    /** 미읽음 알림 존재 여부 */
    @Operation(summary = "미읽음 알림 존재 여부", description = "알림 버튼 빨간 점 표시용")
    @GetMapping("/unread-exists")
    public ApiResponse<NotificationDto.UnreadExists> unreadExists() {
        return ApiResponse.ok(new NotificationDto.UnreadExists(
                notificationService.unreadExists(SecurityUtil.currentUserId())));
    }
}

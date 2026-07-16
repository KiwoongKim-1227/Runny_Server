package com.goodspace.runny.domain.crew.service;

import com.goodspace.runny.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 크루 이벤트 훅 (8단계 실제 구현 연결 완료).
 * 가입 승인: 알림 설정 on인 유저에게 CREW_APPROVED 알림 생성 (판정은 NotificationService 내부).
 */
@Component
@RequiredArgsConstructor
public class CrewNotificationHook {

    private final NotificationService notificationService;

    /** 가입 승인 알림 - 승인 성공 건마다 호출 (CrewAdminService 일괄 승인 루프) */
    public void onJoinApproved(Long userId, Long crewId, String crewName) {
        notificationService.notifyCrewApproved(userId, crewId, crewName);
    }
}

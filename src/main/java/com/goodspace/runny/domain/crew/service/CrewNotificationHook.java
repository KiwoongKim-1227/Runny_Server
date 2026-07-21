package com.goodspace.runny.domain.crew.service;

import com.goodspace.runny.domain.achievement.service.AchievementService;
import com.goodspace.runny.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 크루 이벤트 훅. 가입 승인 시 알림 생성(설정 on 판정은 NotificationService 내부)과
 * "친구들과 달리기" 업적(크루 가입) 판정을 함께 처리한다. 크루 생성자도 크루에 소속되므로 동일 업적 대상.
 */
@Component
@RequiredArgsConstructor
public class CrewNotificationHook {

    private final NotificationService notificationService;
    private final AchievementService achievementService;

    /** 가입 승인 - 승인 성공 건마다 호출 (CrewAdminService 일괄 승인 루프). 알림 + 크루 가입 업적 */
    public void onJoinApproved(Long userId, Long crewId, String crewName) {
        notificationService.notifyCrewApproved(userId, crewId, crewName);
        achievementService.onCrewJoined(userId);
    }

    /** 크루 생성 - 생성자(크루장)도 크루 가입으로 보고 업적 판정 */
    public void onCrewCreated(Long leaderId) {
        achievementService.onCrewJoined(leaderId);
    }
}

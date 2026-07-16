package com.goodspace.runny.domain.friend.service;

import com.goodspace.runny.domain.achievement.service.AchievementService;
import com.goodspace.runny.domain.notification.service.NotificationService;
import com.goodspace.runny.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 친구 이벤트 훅 (8단계 실제 구현 연결 완료).
 * 요청 수신: 설정 on인 수신자에게 FRIEND_REQUEST 알림 생성.
 * 수락: 친구 사귀기 업적 판정(양쪽 모두).
 */
@Component
@RequiredArgsConstructor
public class FriendNotificationHook {

    private final NotificationService notificationService;
    private final AchievementService achievementService;
    private final UserRepository userRepository;

    /** 친구 요청 수신 - 알림 설정 on인 경우만 생성 (판정은 NotificationService 내부) */
    public void onFriendRequestReceived(Long receiverId, Long requesterId) {
        String requesterNickname = userRepository.findById(requesterId)
                .map(user -> user.getNickname())
                .orElse(null);
        notificationService.notifyFriendRequest(receiverId, requesterId, requesterNickname);
    }

    /** 친구 수락 - 친구 사귀기 업적 판정 (요청자/수신자 양쪽 모두) */
    public void onFriendAccepted(Long requesterId, Long receiverId) {
        achievementService.onFriendAccepted(requesterId, receiverId);
    }
}

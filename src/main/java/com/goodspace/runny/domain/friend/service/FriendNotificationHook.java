package com.goodspace.runny.domain.friend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 친구 이벤트 알림 훅. 알림 도메인은 8단계 구현이므로 지금은 이벤트 지점만 정의해 두고,
 * 8단계에서 user_setting(알림 on/off) 확인 + notification 레코드 생성 로직으로 교체한다.
 */
@Slf4j
@Component
public class FriendNotificationHook {

    /** 친구 요청 수신 알림 지점 - TODO(8단계): 설정 on인 수신자에게 FRIEND_REQUEST 알림 생성 */
    public void onFriendRequestReceived(Long receiverId, Long requesterId) {
        log.debug("친구 요청 알림 훅: receiver={}, requester={}", receiverId, requesterId);
    }

    /** 친구 수락 지점 - TODO(8단계): 업적(친구 사귀기) 판정 연결 */
    public void onFriendAccepted(Long requesterId, Long receiverId) {
        log.debug("친구 수락 훅: requester={}, receiver={}", requesterId, receiverId);
    }
}

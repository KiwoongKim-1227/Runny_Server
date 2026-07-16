package com.goodspace.runny.domain.notification.service;

import com.goodspace.runny.domain.notification.entity.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 외부 푸시(FCM) 발송 확장 지점. 기획 문서 4.G대로 푸시 연동은 MVP 범위 외이므로
 * 인앱 알림 생성 직후 호출되는 인터페이스만 정의해 두고 기본 구현은 no-op이다.
 * 실연동 시 firebase-admin 의존성 + 디바이스 토큰 테이블 + FcmPushSender 구현으로 교체한다.
 */
public interface PushNotificationSender {

    /** 알림 레코드 생성 직후 호출 - 타입/메시지 기반 푸시 발송 지점 */
    void send(Long userId, NotificationType type, String message);

    /** 기본 구현 - MVP에서는 발송하지 않고 로그만 남긴다 */
    @Slf4j
    @Component
    class NoOp implements PushNotificationSender {
        @Override
        public void send(Long userId, NotificationType type, String message) {
            // TODO(MVP 범위 외): FCM 연동 - user_device_token 조회 후 firebase-admin으로 발송
            log.debug("푸시 발송 지점(no-op): user={}, type={}", userId, type);
        }
    }
}

package com.goodspace.runny.domain.notification.service;

import com.goodspace.runny.domain.notification.dto.NotificationDto;
import com.goodspace.runny.domain.notification.entity.Notification;
import com.goodspace.runny.domain.notification.entity.NotificationType;
import com.goodspace.runny.domain.notification.repository.NotificationRepository;
import com.goodspace.runny.domain.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * 알림 서비스. 이벤트 발생 시 설정(on/off)을 확인해 알림 레코드를 생성하고,
 * payload(JSON)는 Jackson 3 JsonMapper로 직렬화하며 표시 메시지는 조회 시 타입별로 조립한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SettingService settingService;
    private final PushNotificationSender pushNotificationSender;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** 친구 요청 수신 알림 생성 - 설정 on인 경우만 (6단계 훅에서 호출) */
    @Transactional
    public void notifyFriendRequest(Long receiverId, Long requesterId, String requesterNickname) {
        if (!settingService.getOrCreate(receiverId).isNotifyFriendRequest()) {
            return;
        }
        save(receiverId, NotificationType.FRIEND_REQUEST,
                Map.of("requesterId", requesterId, "requesterNickname", nullSafe(requesterNickname)));
    }

    /** 크루 가입 승인 알림 생성 - 설정 on인 경우만 (7단계 훅에서 호출) */
    @Transactional
    public void notifyCrewApproved(Long userId, Long crewId, String crewName) {
        if (!settingService.getOrCreate(userId).isNotifyCrewApproval()) {
            return;
        }
        save(userId, NotificationType.CREW_APPROVED,
                Map.of("crewId", crewId, "crewName", nullSafe(crewName)));
    }

    /** 알림 목록 조회 - 타입별 메시지 조립 후 일괄 읽음 처리 */
    @Transactional
    public List<NotificationDto.Item> getNotifications(Long userId) {
        List<NotificationDto.Item> items = notificationRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::toItem)
                .toList();
        notificationRepository.markAllRead(userId);
        return items;
    }

    /** 미읽음 알림 존재 여부 (빨간 점) */
    @Transactional(readOnly = true)
    public boolean unreadExists(Long userId) {
        return notificationRepository.existsByUserIdAndReadFalse(userId);
    }

    /** 알림 저장 공통 - payload는 JsonMapper 직렬화. 알림 실패가 본 트랜잭션을 깨지 않도록 예외는 로그만 */
    private void save(Long userId, NotificationType type, Map<String, Object> payload) {
        try {
            Notification notification = notificationRepository
                    .save(new Notification(userId, type, jsonMapper.writeValueAsString(payload)));
            // 외부 푸시 발송 지점 (MVP no-op, FCM 연동 시 교체)
            pushNotificationSender.send(userId, type, toItem(notification).message());
        } catch (Exception e) {
            log.error("알림 생성 실패: user={}, type={}", userId, type, e);
        }
    }

    /**
     * 표시 메시지 조립 - 타입별로 payload를 해석. 신규 타입 추가 시 여기에 케이스만 추가.
     * payload JSON 파싱 실패 시 기본 메시지로 대체해 손상된 레코드 하나가 전체 목록 조회를 막지 않는다.
     */
    private NotificationDto.Item toItem(Notification notification) {
        String message;
        try {
            JsonNode payload = jsonMapper.readTree(notification.getPayload());
            message = switch (notification.getType()) {
                case FRIEND_REQUEST -> payload.path("requesterNickname").asString("알 수 없음")
                        + "님이 친구 요청을 보냈습니다.";
                case CREW_APPROVED -> payload.path("crewName").asString("크루")
                        + " 크루 가입이 승인되었습니다.";
            };
        } catch (Exception e) {
            log.warn("알림 payload 파싱 실패 - 기본 메시지로 대체: id={}, type={}",
                    notification.getId(), notification.getType(), e);
            message = switch (notification.getType()) {
                case FRIEND_REQUEST -> "새로운 친구 요청이 있습니다.";
                case CREW_APPROVED -> "크루 가입이 승인되었습니다.";
            };
        }
        return new NotificationDto.Item(notification.getId(), notification.getType(), message,
                notification.isRead(), notification.getCreatedAt());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

package com.goodspace.runny.domain.crew.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 크루 이벤트 알림 훅. 알림 도메인은 8단계 구현이므로 이벤트 지점만 정의해 두고,
 * 8단계에서 user_setting(크루 승인 알림 on/off) 확인 + notification 레코드 생성 로직으로 교체한다.
 */
@Slf4j
@Component
public class CrewNotificationHook {

    /** 가입 승인 알림 지점 - TODO(8단계): 설정 on인 유저에게 CREW_APPROVED 알림 생성 */
    public void onJoinApproved(Long userId, Long crewId, String crewName) {
        log.debug("크루 승인 알림 훅: user={}, crew={}({})", userId, crewId, crewName);
    }
}

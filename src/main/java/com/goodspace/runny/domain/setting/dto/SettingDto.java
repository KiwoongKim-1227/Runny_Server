package com.goodspace.runny.domain.setting.dto;

import com.goodspace.runny.domain.user.entity.Provider;

/**
 * 설정 요청/응답 DTO 모음.
 */
public final class SettingDto {

    private SettingDto() {
    }

    /** 설정 메인 응답 - provider로 비밀번호 변경 메뉴 노출 여부를 프론트가 분기 (EMAIL일 때만 노출) */
    public record MeResponse(
            String dogName,
            String dogImageUrl,
            String email,
            Provider provider
    ) {
    }

    /** 알림 설정 응답 */
    public record NotificationSettings(
            boolean notifyFriendRequest,
            boolean notifyCrewApproval
    ) {
    }

    /** 알림 설정 변경 요청 - null 필드는 미변경 */
    public record UpdateRequest(
            Boolean notifyFriendRequest,
            Boolean notifyCrewApproval
    ) {
    }
}

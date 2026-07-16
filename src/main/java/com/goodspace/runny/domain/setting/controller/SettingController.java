package com.goodspace.runny.domain.setting.controller;

import com.goodspace.runny.domain.setting.dto.SettingDto;
import com.goodspace.runny.domain.setting.service.SettingService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 설정 API 컨트롤러. 설정 메인과 알림 토글을 제공한다.
 * 개인정보 수정/비밀번호 변경/탈퇴/로그아웃은 회원(User)/인증(Auth) API를 재사용한다.
 */
@Tag(name = "Setting", description = "설정 API - 설정 메인, 알림 토글")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    /** 설정 메인 조회 */
    @Operation(summary = "설정 메인 조회",
            description = "현재 활성 강아지 사진/이름 + 이메일 + provider. provider가 EMAIL일 때만 비밀번호 변경 메뉴 노출")
    @GetMapping("/me")
    public ApiResponse<SettingDto.MeResponse> getSettingMain() {
        return ApiResponse.ok(settingService.getSettingMain(SecurityUtil.currentUserId()));
    }

    /** 알림 설정 조회 */
    @Operation(summary = "알림 설정 조회", description = "친구 요청/크루 승인 알림 토글. 기본값 전부 on")
    @GetMapping("/notifications")
    public ApiResponse<SettingDto.NotificationSettings> getNotificationSettings() {
        return ApiResponse.ok(settingService.getNotificationSettings(SecurityUtil.currentUserId()));
    }

    /** 알림 설정 변경 */
    @Operation(summary = "알림 설정 변경", description = "null 필드는 미변경. off인 항목은 이후 알림이 생성되지 않는다")
    @PatchMapping("/notifications")
    public ApiResponse<SettingDto.NotificationSettings> updateNotificationSettings(
            @RequestBody SettingDto.UpdateRequest request) {
        return ApiResponse.ok(settingService.updateNotificationSettings(SecurityUtil.currentUserId(), request));
    }
}

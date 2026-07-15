package com.goodspace.runny.domain.user.controller;

import com.goodspace.runny.domain.auth.dto.AuthResponse;
import com.goodspace.runny.domain.user.dto.UserDto;
import com.goodspace.runny.domain.user.service.UserService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API 컨트롤러. 온보딩 프로필, 내 정보 조회/수정, 비밀번호 변경, 회원 탈퇴를 제공한다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 닉네임 중복확인 - 사용 가능 여부 반환 (확정 검증은 프로필 저장 시 재수행) */
    @GetMapping("/nickname/check")
    public ApiResponse<AuthResponse.NicknameCheck> checkNickname(@RequestParam String nickname) {
        return ApiResponse.ok(new AuthResponse.NicknameCheck(userService.isNicknameAvailable(nickname)));
    }

    /** 온보딩 프로필 저장 (닉네임/키/성별/몸무게) */
    @PostMapping("/me/profile")
    public ApiResponse<Void> saveProfile(@Valid @RequestBody UserDto.ProfileRequest request) {
        userService.saveProfile(SecurityUtil.currentUserId(), request);
        return ApiResponse.ok();
    }

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ApiResponse<UserDto.MeResponse> getMe() {
        return ApiResponse.ok(userService.getMe(SecurityUtil.currentUserId()));
    }

    /** 내 정보 수정 (닉네임/키/몸무게) */
    @PatchMapping("/me")
    public ApiResponse<UserDto.MeResponse> updateMe(@Valid @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.ok(userService.updateMe(SecurityUtil.currentUserId(), request));
    }

    /** 비밀번호 변경 (자체 가입 유저 전용) */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody UserDto.ChangePasswordRequest request) {
        userService.changePassword(SecurityUtil.currentUserId(), request);
        return ApiResponse.ok();
    }

    /** 회원 탈퇴 (소프트 삭제 + 개인정보 즉시 익명화) */
    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw() {
        userService.withdraw(SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }
}

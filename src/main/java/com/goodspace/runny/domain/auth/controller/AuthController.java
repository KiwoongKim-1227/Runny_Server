package com.goodspace.runny.domain.auth.controller;

import com.goodspace.runny.domain.auth.dto.AuthRequest;
import com.goodspace.runny.domain.auth.dto.AuthResponse;
import com.goodspace.runny.domain.auth.service.AuthService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러. 가입/로그인/소셜/재발급/로그아웃/비밀번호 찾기를 제공한다.
 * /api/auth/** 는 SecurityConfig 화이트리스트로 인증 없이 접근 가능하다(로그아웃 제외 - 토큰 필요 없이도 동작하도록 body 미사용).
 */
@Tag(name = "Auth", description = "인증 API - 가입/로그인/소셜/재발급/로그아웃/비밀번호 찾기")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 가입용 이메일 인증코드 발송 */
    @Operation(summary = "가입용 이메일 인증코드 발송", description = "6자리 코드, 5분 유효. 이미 가입된 이메일은 USER_001/USER_007")
    @PostMapping("/email/send-code")
    public ApiResponse<Void> sendSignupCode(@Valid @RequestBody AuthRequest.SendCode request) {
        authService.sendSignupCode(request.email());
        return ApiResponse.ok();
    }

    /** 가입용 인증코드 검증 */
    @Operation(summary = "가입용 인증코드 검증", description = "불일치 AUTH_005, 만료 AUTH_006")
    @PostMapping("/email/verify-code")
    public ApiResponse<AuthResponse.CodeVerified> verifySignupCode(
            @Valid @RequestBody AuthRequest.VerifyCode request) {
        return ApiResponse.ok(authService.verifySignupCode(request));
    }

    /** 이메일 가입 (약관 필수 3종 + 선택 마케팅) */
    @Operation(summary = "이메일 가입", description = "인증 완료 상태 필요. 필수 약관 3종 동의, 비밀번호 규칙(8자+, 영문/숫자/특수문자). 가입 즉시 토큰 발급")
    @PostMapping("/signup")
    public ApiResponse<AuthResponse.Tokens> signup(@Valid @RequestBody AuthRequest.Signup request) {
        return ApiResponse.ok(authService.signup(request));
    }

    /** 이메일 로그인 */
    @Operation(summary = "이메일 로그인", description = "자격 불일치 시 AUTH_012 단일 응답")
    @PostMapping("/login")
    public ApiResponse<AuthResponse.Tokens> login(@Valid @RequestBody AuthRequest.Login request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** 소셜 로그인/가입 통합 (isNewUser 분기) */
    @Operation(summary = "소셜 로그인/가입 통합", description = "기존 유저: isNewUser=false+토큰 / 신규+약관 미포함: isNewUser=true, tokens=null / 신규+약관 포함: 가입 후 토큰")
    @PostMapping("/social/login")
    public ApiResponse<AuthResponse.SocialLogin> socialLogin(
            @Valid @RequestBody AuthRequest.SocialLogin request) {
        return ApiResponse.ok(authService.socialLogin(request));
    }

    /** 토큰 재발급 */
    @Operation(summary = "토큰 재발급", description = "refresh 토큰 검증 후 access/refresh 회전 발급")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse.Tokens> refresh(@Valid @RequestBody AuthRequest.Refresh request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    /** 로그아웃 - 서버 refresh 토큰 무효화 (access 헤더 필요) */
    @Operation(summary = "로그아웃", description = "서버 refresh 토큰 무효화. Authorization 헤더의 access 토큰 필요")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    /** 비밀번호 찾기 1단계 - 인증코드 발송 */
    @Operation(summary = "비밀번호 찾기 1단계 - 코드 발송", description = "자체 가입(EMAIL) 유저만 가능. 소셜 유저는 USER_008")
    @PostMapping("/password/send-code")
    public ApiResponse<Void> sendPasswordResetCode(@Valid @RequestBody AuthRequest.SendCode request) {
        authService.sendPasswordResetCode(request.email());
        return ApiResponse.ok();
    }

    /** 비밀번호 찾기 2단계 - 코드 검증 + reset 토큰 발급 */
    @Operation(summary = "비밀번호 찾기 2단계 - 코드 검증", description = "성공 시 resetToken 반환(10분 유효)")
    @PostMapping("/password/verify-code")
    public ApiResponse<AuthResponse.CodeVerified> verifyPasswordResetCode(
            @Valid @RequestBody AuthRequest.VerifyCode request) {
        return ApiResponse.ok(authService.verifyPasswordResetCode(request));
    }

    /** 비밀번호 찾기 3단계 - 새 비밀번호 설정 */
    @Operation(summary = "비밀번호 찾기 3단계 - 재설정", description = "resetToken 검증 후 새 비밀번호 저장, 기존 refresh 무효화")
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthRequest.PasswordReset request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }
}

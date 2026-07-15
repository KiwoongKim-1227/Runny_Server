package com.goodspace.runny.domain.auth.controller;

import com.goodspace.runny.domain.auth.dto.AuthRequest;
import com.goodspace.runny.domain.auth.dto.AuthResponse;
import com.goodspace.runny.domain.auth.service.AuthService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
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
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 가입용 이메일 인증코드 발송 */
    @PostMapping("/email/send-code")
    public ApiResponse<Void> sendSignupCode(@Valid @RequestBody AuthRequest.SendCode request) {
        authService.sendSignupCode(request.email());
        return ApiResponse.ok();
    }

    /** 가입용 인증코드 검증 */
    @PostMapping("/email/verify-code")
    public ApiResponse<AuthResponse.CodeVerified> verifySignupCode(
            @Valid @RequestBody AuthRequest.VerifyCode request) {
        return ApiResponse.ok(authService.verifySignupCode(request));
    }

    /** 이메일 가입 (약관 필수 3종 + 선택 마케팅) */
    @PostMapping("/signup")
    public ApiResponse<AuthResponse.Tokens> signup(@Valid @RequestBody AuthRequest.Signup request) {
        return ApiResponse.ok(authService.signup(request));
    }

    /** 이메일 로그인 */
    @PostMapping("/login")
    public ApiResponse<AuthResponse.Tokens> login(@Valid @RequestBody AuthRequest.Login request) {
        return ApiResponse.ok(authService.login(request));
    }

    /** 소셜 로그인/가입 통합 (isNewUser 분기) */
    @PostMapping("/social/login")
    public ApiResponse<AuthResponse.SocialLogin> socialLogin(
            @Valid @RequestBody AuthRequest.SocialLogin request) {
        return ApiResponse.ok(authService.socialLogin(request));
    }

    /** 토큰 재발급 */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse.Tokens> refresh(@Valid @RequestBody AuthRequest.Refresh request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    /** 로그아웃 - 서버 refresh 토큰 무효화 (access 헤더 필요) */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    /** 비밀번호 찾기 1단계 - 인증코드 발송 */
    @PostMapping("/password/send-code")
    public ApiResponse<Void> sendPasswordResetCode(@Valid @RequestBody AuthRequest.SendCode request) {
        authService.sendPasswordResetCode(request.email());
        return ApiResponse.ok();
    }

    /** 비밀번호 찾기 2단계 - 코드 검증 + reset 토큰 발급 */
    @PostMapping("/password/verify-code")
    public ApiResponse<AuthResponse.CodeVerified> verifyPasswordResetCode(
            @Valid @RequestBody AuthRequest.VerifyCode request) {
        return ApiResponse.ok(authService.verifyPasswordResetCode(request));
    }

    /** 비밀번호 찾기 3단계 - 새 비밀번호 설정 */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthRequest.PasswordReset request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }
}

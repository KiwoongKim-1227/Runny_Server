package com.goodspace.runny.domain.auth.dto;

import com.goodspace.runny.domain.user.entity.OnboardingStatus;
import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.domain.user.entity.User;

/**
 * 인증 도메인 응답 DTO 모음.
 */
public final class AuthResponse {

    private AuthResponse() {
    }

    /** 로그인/가입 공통 응답 - 토큰 + 온보딩 상태 + provider */
    public record Tokens(
            String accessToken,
            String refreshToken,
            OnboardingStatus onboardingStatus,
            Provider provider
    ) {
        public static Tokens of(String accessToken, String refreshToken, User user) {
            return new Tokens(accessToken, refreshToken, user.getOnboardingStatus(), user.getProvider());
        }
    }

    /**
     * 소셜 로그인 통합 응답.
     * 기존 유저: isNewUser=false + 토큰. 신규(약관 미동의): isNewUser=true + 토큰 null (프론트가 약관 화면으로 분기).
     * 신규(약관 동의 포함 요청): 가입 처리 후 isNewUser=true + 토큰.
     */
    public record SocialLogin(
            boolean isNewUser,
            Tokens tokens
    ) {
    }

    /** 인증코드 검증 응답 - 비밀번호 찾기의 경우 resetToken 포함 */
    public record CodeVerified(
            boolean verified,
            String resetToken
    ) {
    }

    /** 닉네임 중복확인 응답 */
    public record NicknameCheck(
            boolean available
    ) {
    }
}

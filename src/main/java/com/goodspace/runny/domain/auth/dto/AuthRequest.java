package com.goodspace.runny.domain.auth.dto;

import com.goodspace.runny.domain.user.entity.Provider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 인증 도메인 요청 DTO 모음. record 기반으로 정의한다.
 */
public final class AuthRequest {

    private AuthRequest() {
    }

    /** 약관 동의 값 - 필수 3종(서비스/개인정보/위치정보) + 선택(마케팅) */
    public record TermsAgreement(
            @NotNull Boolean serviceTerms,
            @NotNull Boolean privacyTerms,
            @NotNull Boolean locationTerms,
            Boolean marketingTerms
    ) {
        /** 필수 3종 모두 동의 여부 */
        public boolean allRequiredAgreed() {
            return Boolean.TRUE.equals(serviceTerms)
                    && Boolean.TRUE.equals(privacyTerms)
                    && Boolean.TRUE.equals(locationTerms);
        }

        /** 마케팅(선택) 동의 여부 */
        public boolean marketingAgreed() {
            return Boolean.TRUE.equals(marketingTerms);
        }
    }

    /** 인증코드 발송 요청 (가입/비밀번호 찾기 공용) */
    public record SendCode(
            @NotBlank @Email String email
    ) {
    }

    /** 인증코드 검증 요청 */
    public record VerifyCode(
            @NotBlank @Email String email,
            @NotBlank String code
    ) {
    }

    /** 이메일 가입 요청 */
    public record Signup(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String passwordConfirm,
            @NotNull TermsAgreement terms
    ) {
    }

    /** 이메일 로그인 요청 */
    public record Login(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    /** 소셜 로그인/가입 통합 요청. 신규 가입 시에만 terms 필요 */
    public record SocialLogin(
            @NotNull Provider provider,
            @NotBlank String token,
            TermsAgreement terms
    ) {
    }

    /** 토큰 재발급 요청 */
    public record Refresh(
            @NotBlank String refreshToken
    ) {
    }

    /** 비밀번호 재설정 요청 (reset 토큰 기반) */
    public record PasswordReset(
            @NotBlank @Email String email,
            @NotBlank String resetToken,
            @NotBlank String newPassword,
            @NotBlank String newPasswordConfirm
    ) {
    }
}

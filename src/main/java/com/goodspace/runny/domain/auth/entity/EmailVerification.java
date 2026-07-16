package com.goodspace.runny.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 이메일 인증코드 엔티티. 가입(SIGNUP)과 비밀번호 찾기(PASSWORD_RESET)에 공용으로 사용한다.
 * 비밀번호 찾기는 코드 검증 성공 시 임시 reset 토큰을 발급해 재설정 단계에서 사용한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification")
public class EmailVerification {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified;

    // 이 코드에 대한 검증 실패 횟수 (Attempt Limit 판정용 - 10분 창 내 합산)
    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 비밀번호 재설정용 임시 토큰 (PASSWORD_RESET 검증 성공 시 발급)
    @Column(name = "reset_token", length = 36)
    private String resetToken;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    public EmailVerification(String email, String code, VerificationType type, int expiryMinutes) {
        this.email = email;
        this.code = code;
        this.type = type;
        this.expiresAt = LocalDateTime.now(ZONE_SEOUL).plusMinutes(expiryMinutes);
        this.verified = false;
        this.failCount = 0;
        this.createdAt = LocalDateTime.now(ZONE_SEOUL);
    }

    /** 검증 실패 횟수 증가 (Attempt Limit) */
    public void increaseFailCount() {
        this.failCount++;
    }

    /** 코드 만료 여부 */
    public boolean isExpired() {
        return LocalDateTime.now(ZONE_SEOUL).isAfter(this.expiresAt);
    }

    /** 코드 검증 성공 처리 */
    public void markVerified() {
        this.verified = true;
    }

    /** 비밀번호 재설정 토큰 발급 (10분 유효) */
    public String issueResetToken() {
        this.resetToken = UUID.randomUUID().toString();
        this.resetTokenExpiresAt = LocalDateTime.now(ZONE_SEOUL).plusMinutes(RESET_TOKEN_EXPIRY_MINUTES);
        return this.resetToken;
    }

    /** 재설정 토큰 유효성 확인 */
    public boolean isResetTokenValid(String token) {
        return this.resetToken != null
                && this.resetToken.equals(token)
                && this.resetTokenExpiresAt != null
                && LocalDateTime.now(ZONE_SEOUL).isBefore(this.resetTokenExpiresAt);
    }

    /** 재설정 완료 후 토큰 무효화 (재사용 방지) */
    public void invalidateResetToken() {
        this.resetToken = null;
        this.resetTokenExpiresAt = null;
    }
}

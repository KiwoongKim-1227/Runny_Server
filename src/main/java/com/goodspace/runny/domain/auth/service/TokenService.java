package com.goodspace.runny.domain.auth.service;

import com.goodspace.runny.domain.auth.dto.AuthResponse;
import com.goodspace.runny.domain.auth.entity.RefreshToken;
import com.goodspace.runny.domain.auth.repository.RefreshTokenRepository;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * JWT 발급과 refresh 토큰 DB 관리(1유저 1토큰 upsert)를 담당하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiryMillis;

    /** access/refresh 발급 + refresh를 DB에 upsert 저장 후 공통 토큰 응답 생성 */
    @Transactional
    public AuthResponse.Tokens issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        LocalDateTime expiresAt = LocalDateTime.now(ZONE_SEOUL).plusSeconds(refreshTokenExpiryMillis / 1000);

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        existing -> existing.rotate(refreshToken, expiresAt),
                        () -> refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken, expiresAt)));

        return AuthResponse.Tokens.of(accessToken, refreshToken, user);
    }

    /** 로그아웃/탈퇴 시 refresh 토큰 삭제 */
    @Transactional
    public void deleteRefreshToken(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}

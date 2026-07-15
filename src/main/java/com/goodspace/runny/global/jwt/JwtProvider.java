package com.goodspace.runny.global.jwt;

import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT access/refresh 토큰의 발급과 검증을 담당한다. HS256 서명, subject에 userId 저장.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TYPE_ACCESS = "ACCESS";
    public static final String TYPE_REFRESH = "REFRESH";

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
                       @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    /** access 토큰 발급 (기본 1시간) */
    public String createAccessToken(Long userId) {
        return createToken(userId, TYPE_ACCESS, accessTokenExpiry);
    }

    /** refresh 토큰 발급 (기본 14일) */
    public String createRefreshToken(Long userId) {
        return createToken(userId, TYPE_REFRESH, refreshTokenExpiry);
    }

    /** 토큰 생성 공통 로직 - subject에 userId, 별도 클레임에 토큰 타입 저장 */
    private String createToken(Long userId, String tokenType, long expiryMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMillis))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /** 토큰 검증 후 userId 추출. 만료/위조 시 각각 AUTH_004/AUTH_003 예외 */
    public Long parseUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /** 토큰이 refresh 타입인지 확인 (재발급 API에서 access 토큰 오용 방지) */
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    /** 토큰이 access 타입인지 확인 (보호 API에서 refresh 토큰 오용 방지) */
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class));
    }

    /** 서명 검증 및 클레임 파싱 공통 처리 */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.AUTH_004);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_003);
        }
    }
}

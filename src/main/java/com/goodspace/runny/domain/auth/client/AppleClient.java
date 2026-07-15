package com.goodspace.runny.domain.auth.client;

import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플 identity token(JWT) 검증 클라이언트. appleid.apple.com/auth/keys의 공개키(JWK)로
 * 서명을 검증하고 iss/aud 클레임을 확인한다. spring-security-oauth2-jose(Nimbus) 사용.
 */
@Slf4j
@Component
public class AppleClient {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_JWK_SET_URI = "https://appleid.apple.com/auth/keys";

    private final JwtDecoder jwtDecoder;

    public AppleClient(@Value("${oauth.apple.client-id}") String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(APPLE_JWK_SET_URI).build();
        // 기본 검증(만료 등) + iss + aud(번들 ID) 클레임 검증
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(clientId));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(APPLE_ISSUER), audienceValidator));
        this.jwtDecoder = decoder;
    }

    /** identity token 서명/클레임 검증 후 sub/email 추출. 실패 시 AUTH_010 */
    public SocialUserInfo verify(String identityToken) {
        try {
            Jwt jwt = jwtDecoder.decode(identityToken);
            String sub = jwt.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new BusinessException(ErrorCode.AUTH_010);
            }
            String email = jwt.getClaimAsString("email");
            return new SocialUserInfo(Provider.APPLE, sub, email);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("애플 identity token 검증 실패", e);
            throw new BusinessException(ErrorCode.AUTH_010);
        }
    }
}

package com.goodspace.runny.domain.auth.client;

import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * provider별 소셜 토큰 검증 라우터. 서비스 계층은 이 클래스 하나만 의존한다.
 */
@Component
@RequiredArgsConstructor
public class SocialTokenVerifier {

    private final KakaoClient kakaoClient;
    private final GoogleClient googleClient;
    private final AppleClient appleClient;

    /** provider에 맞는 클라이언트로 토큰 검증 후 사용자 정보 반환 */
    public SocialUserInfo verify(Provider provider, String token) {
        return switch (provider) {
            case KAKAO -> kakaoClient.verify(token);
            case GOOGLE -> googleClient.verify(token);
            case APPLE -> appleClient.verify(token);
            case EMAIL -> throw new BusinessException(ErrorCode.COMMON_001, "소셜 provider가 아닙니다.");
        };
    }
}

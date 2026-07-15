package com.goodspace.runny.domain.auth.client;

import com.goodspace.runny.domain.user.entity.Provider;

/**
 * 소셜 플랫폼에서 검증/조회한 사용자 식별 정보. provider + providerId로 유저를 판별한다.
 */
public record SocialUserInfo(
        Provider provider,
        String providerId,
        String email
) {
}

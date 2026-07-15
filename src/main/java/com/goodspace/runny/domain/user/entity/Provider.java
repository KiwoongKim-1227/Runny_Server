package com.goodspace.runny.domain.user.entity;

/**
 * 가입 경로 구분. EMAIL은 자체 가입, 나머지는 소셜 가입이다.
 */
public enum Provider {
    EMAIL, KAKAO, GOOGLE, APPLE
}

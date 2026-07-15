package com.goodspace.runny.domain.user.entity;

/**
 * 온보딩 진행 상태. COMPLETED 전에는 메인(놀이터) 진입이 차단된다.
 */
public enum OnboardingStatus {
    PROFILE_REQUIRED, DOG_REQUIRED, COMPLETED
}

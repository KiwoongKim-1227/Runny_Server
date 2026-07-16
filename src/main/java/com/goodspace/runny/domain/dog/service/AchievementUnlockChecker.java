package com.goodspace.runny.domain.dog.service;

/**
 * 업적 해금형 견종의 해금 여부 판정 인터페이스.
 * 실제 구현은 업적 도메인의 AchievementUnlockCheckerImpl(8단계) - 업적 보상 수령(claimed) 기준으로 판정한다.
 */
public interface AchievementUnlockChecker {

    /** 해당 업적 코드의 보상(견종 해금)을 유저가 수령했는지 여부 */
    boolean isUnlocked(Long userId, String achievementCode);
}

package com.goodspace.runny.domain.achievement.service;

import com.goodspace.runny.domain.achievement.repository.UserAchievementRepository;
import com.goodspace.runny.domain.dog.service.AchievementUnlockChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 견종 해금 판정 실제 구현 (3단계 인터페이스 교체). 업적 보상 수령(claimed=true) 시점을 해금으로 본다.
 */
@Component
@RequiredArgsConstructor
public class AchievementUnlockCheckerImpl implements AchievementUnlockChecker {

    private final UserAchievementRepository userAchievementRepository;

    @Override
    public boolean isUnlocked(Long userId, String achievementCode) {
        return userAchievementRepository.existsClaimedByCode(userId, achievementCode);
    }
}

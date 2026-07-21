package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.AchievementProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 업적 진행 카운터 리포지토리.
 */
public interface AchievementProgressRepository extends JpaRepository<AchievementProgress, Long> {

    Optional<AchievementProgress> findByUserIdAndCode(Long userId, String code);
}

package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 유저 업적 리포지토리.
 */
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

    boolean existsByUserIdAndAchievementId(Long userId, Long achievementId);

    /** 견종 해금 여부 판정 - 업적 코드 기준, 보상 수령(claimed) 완료 여부 */
    @Query("SELECT COUNT(ua) > 0 FROM UserAchievement ua " +
            "WHERE ua.userId = :userId AND ua.achievement.code = :code AND ua.claimed = true")
    boolean existsClaimedByCode(@Param("userId") Long userId, @Param("code") String code);
}

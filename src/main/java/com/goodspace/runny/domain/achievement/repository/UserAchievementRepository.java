package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 보상 수령 조건부 처리 - 영향 행 1인 요청만 보상 지급 주체 (동시 요청 중복 지급 차단).
     * clearAutomatically=true로 벌크 UPDATE 후 영속성 컨텍스트를 비워 stale 데이터 방지.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserAchievement ua SET ua.claimed = true " +
            "WHERE ua.userId = :userId AND ua.achievement.id = :achievementId AND ua.claimed = false")
    int claimIfNotClaimed(@Param("userId") Long userId, @Param("achievementId") Long achievementId);

    /** 견종 해금 여부 판정 - 업적 코드 기준, 보상 수령(claimed) 완료 여부 */
    @Query("SELECT COUNT(ua) > 0 FROM UserAchievement ua " +
            "WHERE ua.userId = :userId AND ua.achievement.code = :code AND ua.claimed = true")
    boolean existsClaimedByCode(@Param("userId") Long userId, @Param("code") String code);
}

package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.UserLandmarkVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 랜드마크 방문 기록 리포지토리.
 */
public interface UserLandmarkVisitRepository extends JpaRepository<UserLandmarkVisit, Long> {

    List<UserLandmarkVisit> findByUserId(Long userId);

    boolean existsByUserIdAndLandmarkId(Long userId, Long landmarkId);

    int countByUserId(Long userId);
}

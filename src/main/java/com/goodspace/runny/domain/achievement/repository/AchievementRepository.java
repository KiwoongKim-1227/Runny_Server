package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 업적 마스터 리포지토리.
 */
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByCode(String code);

    List<Achievement> findAllByOrderByIdAsc();
}

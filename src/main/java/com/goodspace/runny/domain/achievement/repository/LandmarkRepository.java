package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.Landmark;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 랜드마크 마스터 리포지토리.
 */
public interface LandmarkRepository extends JpaRepository<Landmark, Long> {
}

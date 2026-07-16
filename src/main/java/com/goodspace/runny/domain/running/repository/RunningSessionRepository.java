package com.goodspace.runny.domain.running.repository;

import com.goodspace.runny.domain.running.entity.RunningSession;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 러닝 세션 리포지토리.
 */
public interface RunningSessionRepository extends JpaRepository<RunningSession, Long> {
}

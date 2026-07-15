package com.goodspace.runny.domain.auth.repository;

import com.goodspace.runny.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * refresh 토큰 리포지토리. 1유저 1토큰 정책.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}

package com.goodspace.runny.domain.user.repository;

import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 리포지토리. @SQLRestriction으로 모든 조회에 deleted_at IS NULL이 자동 적용된다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}

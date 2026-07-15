package com.goodspace.runny.domain.user.repository;

import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 회원 리포지토리. @SQLRestriction으로 모든 조회에 deleted_at IS NULL이 자동 적용된다.
 * 코인 증감은 조건부 UPDATE로 DB가 원자성을 보장한다 (벌크 쿼리는 @SQLRestriction 미적용이라 조건 직접 명시).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    /** 닉네임 부분 일치 검색 (본인 제외) - 친구 검색용. 탈퇴 유저는 @SQLRestriction으로 자동 제외 */
    org.springframework.data.domain.Page<User> findByNicknameContainingAndIdNot(
            String nickname, Long id, org.springframework.data.domain.Pageable pageable);

    /** 코인 조건부 차감 - 잔액이 충분할 때만 성공(영향 행 1), 부족하면 영향 행 0 */
    @Modifying
    @Query("UPDATE User u SET u.coin = u.coin - :amount " +
            "WHERE u.id = :userId AND u.coin >= :amount AND u.deletedAt IS NULL")
    int deductCoin(@Param("userId") Long userId, @Param("amount") int amount);

    /** 코인 적립 - UPDATE 방식으로 동시성 안전하게 가산 */
    @Modifying
    @Query("UPDATE User u SET u.coin = u.coin + :amount " +
            "WHERE u.id = :userId AND u.deletedAt IS NULL")
    int addCoin(@Param("userId") Long userId, @Param("amount") int amount);
}

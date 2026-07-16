package com.goodspace.runny.domain.auth.repository;

import com.goodspace.runny.domain.auth.entity.EmailVerification;
import com.goodspace.runny.domain.auth.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 이메일 인증코드 리포지토리. 이메일+용도별 최신 코드 1건을 기준으로 검증하며,
 * 10분 창 기준 발송 횟수(Rate Limit)와 검증 실패 횟수 합산(Attempt Limit)을 제공한다.
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailAndTypeOrderByIdDesc(String email, VerificationType type);

    void deleteByEmailAndType(String email, VerificationType type);

    /** 기준 시각 이후 발송 횟수 - Rate Limit(10분 7회) 판정 */
    int countByEmailAndTypeAndCreatedAtAfter(String email, VerificationType type, LocalDateTime since);

    /** 기준 시각 이후 발송된 코드들의 검증 실패 횟수 합산 - Attempt Limit(10분 7회) 판정 */
    @Query("SELECT COALESCE(SUM(v.failCount), 0) FROM EmailVerification v " +
            "WHERE v.email = :email AND v.type = :type AND v.createdAt > :since")
    int sumFailCountSince(@Param("email") String email,
                          @Param("type") VerificationType type,
                          @Param("since") LocalDateTime since);
}

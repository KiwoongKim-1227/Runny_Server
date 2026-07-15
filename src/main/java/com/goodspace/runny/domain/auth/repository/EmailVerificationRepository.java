package com.goodspace.runny.domain.auth.repository;

import com.goodspace.runny.domain.auth.entity.EmailVerification;
import com.goodspace.runny.domain.auth.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 이메일 인증코드 리포지토리. 이메일+용도별 최신 코드 1건을 기준으로 검증한다.
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailAndTypeOrderByIdDesc(String email, VerificationType type);

    void deleteByEmailAndType(String email, VerificationType type);
}

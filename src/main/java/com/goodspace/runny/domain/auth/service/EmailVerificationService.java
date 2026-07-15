package com.goodspace.runny.domain.auth.service;

import com.goodspace.runny.domain.auth.entity.EmailVerification;
import com.goodspace.runny.domain.auth.entity.VerificationType;
import com.goodspace.runny.domain.auth.repository.EmailVerificationRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.util.VerificationMailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * 이메일 인증코드 발송/검증 서비스. 가입과 비밀번호 찾기 양쪽에서 공용으로 사용한다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final VerificationMailSender verificationMailSender;

    @Value("${mail.verification-code-expiry-minutes}")
    private int expiryMinutes;

    /** 6자리 인증코드 생성 후 저장 및 메일 발송 (기존 코드는 무효화를 위해 최신 건만 검증 대상) */
    @Transactional
    public void sendCode(String email, VerificationType type) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        emailVerificationRepository.save(new EmailVerification(email, code, type, expiryMinutes));
        verificationMailSender.sendVerificationCode(email, code);
    }

    /** 최신 인증코드와 대조 검증. 불일치 AUTH_005, 만료 AUTH_006 */
    @Transactional
    public EmailVerification verifyCode(String email, String code, VerificationType type) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndTypeOrderByIdDesc(email, type)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_005));
        if (verification.isExpired()) {
            throw new BusinessException(ErrorCode.AUTH_006);
        }
        if (!verification.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.AUTH_005);
        }
        verification.markVerified();
        return verification;
    }

    /** 가입 진행 전 "코드 검증 완료" 상태인지 확인 (검증 없이 가입 API 직행 방지) */
    @Transactional(readOnly = true)
    public void assertVerified(String email, VerificationType type) {
        boolean verified = emailVerificationRepository
                .findTopByEmailAndTypeOrderByIdDesc(email, type)
                .map(v -> v.isVerified() && !v.isExpired())
                .orElse(false);
        if (!verified) {
            throw new BusinessException(ErrorCode.AUTH_007);
        }
    }

    /** 최신 인증 레코드 조회 (reset 토큰 검증 단계에서 사용). 없으면 AUTH_014 */
    @Transactional(readOnly = true)
    public EmailVerification findLatest(String email, VerificationType type) {
        return emailVerificationRepository
                .findTopByEmailAndTypeOrderByIdDesc(email, type)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_014));
    }

    /** 사용 완료된 인증코드 정리 (가입/재설정 완료 시 호출) */
    @Transactional
    public void clear(String email, VerificationType type) {
        emailVerificationRepository.deleteByEmailAndType(email, type);
    }
}

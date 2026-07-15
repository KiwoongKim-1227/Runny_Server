package com.goodspace.runny.domain.auth.service;

import com.goodspace.runny.domain.auth.client.SocialTokenVerifier;
import com.goodspace.runny.domain.auth.client.SocialUserInfo;
import com.goodspace.runny.domain.auth.dto.AuthRequest;
import com.goodspace.runny.domain.auth.dto.AuthResponse;
import com.goodspace.runny.domain.auth.entity.EmailVerification;
import com.goodspace.runny.domain.auth.entity.VerificationType;
import com.goodspace.runny.domain.auth.repository.RefreshTokenRepository;
import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스. 이메일 가입/로그인, 소셜 로그인 통합, 토큰 재발급, 로그아웃, 비밀번호 찾기를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final TokenService tokenService;
    private final SocialTokenVerifier socialTokenVerifier;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /** 가입용 인증코드 발송. 이미 가입된 이메일이면 발송 전 차단 */
    @Transactional
    public void sendSignupCode(String email) {
        validateEmailNotRegistered(email);
        emailVerificationService.sendCode(email, VerificationType.SIGNUP);
    }

    /** 가입용 인증코드 검증 */
    @Transactional
    public AuthResponse.CodeVerified verifySignupCode(AuthRequest.VerifyCode request) {
        emailVerificationService.verifyCode(request.email(), request.code(), VerificationType.SIGNUP);
        return new AuthResponse.CodeVerified(true, null);
    }

    /** 이메일 가입. 인증 완료 + 비밀번호 규칙 + 필수 약관 검증 후 생성, 토큰 즉시 발급 */
    @Transactional
    public AuthResponse.Tokens signup(AuthRequest.Signup request) {
        emailVerificationService.assertVerified(request.email(), VerificationType.SIGNUP);
        validateEmailNotRegistered(request.email());
        PasswordValidator.validate(request.password(), request.passwordConfirm());
        validateRequiredTerms(request.terms());

        User user = userRepository.save(User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(Provider.EMAIL)
                .marketingAgreed(request.terms().marketingAgreed())
                .build());
        emailVerificationService.clear(request.email(), VerificationType.SIGNUP);
        return tokenService.issueTokens(user);
    }

    /** 이메일 로그인. 자격 불일치는 이메일/비밀번호를 구분하지 않고 AUTH_012 단일 응답 */
    @Transactional
    public AuthResponse.Tokens login(AuthRequest.Login request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_012));
        if (!user.isEmailProvider() || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_012);
        }
        return tokenService.issueTokens(user);
    }

    /**
     * 소셜 로그인/가입 통합 처리.
     * 기존 유저: 즉시 토큰 발급. 신규 + 약관 미포함: isNewUser=true만 반환(프론트가 약관 화면 분기).
     * 신규 + 필수 약관 동의: 가입 후 토큰 발급.
     */
    @Transactional
    public AuthResponse.SocialLogin socialLogin(AuthRequest.SocialLogin request) {
        SocialUserInfo info = socialTokenVerifier.verify(request.provider(), request.token());

        User existing = userRepository
                .findByProviderAndProviderId(info.provider(), info.providerId())
                .orElse(null);
        if (existing != null) {
            return new AuthResponse.SocialLogin(false, tokenService.issueTokens(existing));
        }

        // 동일 이메일이 다른 방식으로 이미 가입된 경우 차단
        if (info.email() != null && userRepository.existsByEmail(info.email())) {
            throw new BusinessException(ErrorCode.USER_007);
        }

        // 약관 동의값이 없으면 가입 미처리 상태로 신규 여부만 알려준다
        if (request.terms() == null) {
            return new AuthResponse.SocialLogin(true, null);
        }
        validateRequiredTerms(request.terms());

        User user = userRepository.save(User.builder()
                .email(info.email() != null ? info.email() : syntheticEmail(info))
                .provider(info.provider())
                .providerId(info.providerId())
                .marketingAgreed(request.terms().marketingAgreed())
                .build());
        return new AuthResponse.SocialLogin(true, tokenService.issueTokens(user));
    }

    /** refresh 토큰 재발급. 토큰 타입/DB 일치 검증 후 회전(rotate) 방식으로 재발급 */
    @Transactional
    public AuthResponse.Tokens refresh(AuthRequest.Refresh request) {
        String refreshToken = request.refreshToken();
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_011);
        }
        Long userId = jwtProvider.parseUserId(refreshToken);
        boolean matched = refreshTokenRepository.findByUserId(userId)
                .map(saved -> saved.getToken().equals(refreshToken))
                .orElse(false);
        if (!matched) {
            throw new BusinessException(ErrorCode.AUTH_011);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
        return tokenService.issueTokens(user);
    }

    /** 로그아웃 - 서버 refresh 토큰 무효화 (access 삭제는 프론트 책임) */
    @Transactional
    public void logout(Long userId) {
        tokenService.deleteRefreshToken(userId);
    }

    /** 비밀번호 찾기 1단계 - 자체 가입(EMAIL) 유저에게만 코드 발송 */
    @Transactional
    public void sendPasswordResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
        if (!user.isEmailProvider()) {
            throw new BusinessException(ErrorCode.USER_008);
        }
        emailVerificationService.sendCode(email, VerificationType.PASSWORD_RESET);
    }

    /** 비밀번호 찾기 2단계 - 코드 검증 성공 시 임시 reset 토큰 발급 */
    @Transactional
    public AuthResponse.CodeVerified verifyPasswordResetCode(AuthRequest.VerifyCode request) {
        EmailVerification verification = emailVerificationService
                .verifyCode(request.email(), request.code(), VerificationType.PASSWORD_RESET);
        return new AuthResponse.CodeVerified(true, verification.issueResetToken());
    }

    /** 비밀번호 찾기 3단계 - reset 토큰 검증 후 새 비밀번호 설정 */
    @Transactional
    public void resetPassword(AuthRequest.PasswordReset request) {
        EmailVerification verification = emailVerificationService
                .findLatest(request.email(), VerificationType.PASSWORD_RESET);
        if (!verification.isResetTokenValid(request.resetToken())) {
            throw new BusinessException(ErrorCode.AUTH_014);
        }
        PasswordValidator.validate(request.newPassword(), request.newPasswordConfirm());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
        if (!user.isEmailProvider()) {
            throw new BusinessException(ErrorCode.USER_008);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        verification.invalidateResetToken();
        emailVerificationService.clear(request.email(), VerificationType.PASSWORD_RESET);
        // 비밀번호 변경 후 기존 세션 무효화
        tokenService.deleteRefreshToken(user.getId());
    }

    /** 이메일 중복 가입 차단 - 소셜 가입 이메일이면 별도 코드로 안내 */
    private void validateEmailNotRegistered(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailProvider()) {
                throw new BusinessException(ErrorCode.USER_001);
            }
            throw new BusinessException(ErrorCode.USER_007);
        });
    }

    /** 필수 약관 3종 전부 동의 검증 */
    private void validateRequiredTerms(AuthRequest.TermsAgreement terms) {
        if (terms == null || !terms.allRequiredAgreed()) {
            throw new BusinessException(ErrorCode.AUTH_009);
        }
    }

    /** 소셜 플랫폼이 이메일을 주지 않는 경우 대체 식별용 이메일 생성 */
    private String syntheticEmail(SocialUserInfo info) {
        return info.provider().name().toLowerCase() + "_" + info.providerId() + "@social.runny";
    }
}

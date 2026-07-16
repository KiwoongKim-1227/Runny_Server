package com.goodspace.runny.domain.user.service;

import com.goodspace.runny.domain.auth.service.PasswordValidator;
import com.goodspace.runny.domain.auth.service.TokenService;
import com.goodspace.runny.domain.crew.service.CrewService;
import com.goodspace.runny.domain.friend.service.FriendService;
import com.goodspace.runny.domain.user.dto.UserDto;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.util.ProfanityFilter;
import com.goodspace.runny.global.util.S3Uploader;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 회원 서비스. 온보딩 프로필, 내 정보 조회/수정, 비밀번호 변경, 회원 탈퇴(소프트 삭제)를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    // 닉네임 규칙: 2~7자, 한글/영문/숫자만
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]{2,7}$");

    private final UserRepository userRepository;
    private final ProfanityFilter profanityFilter;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final FriendService friendService;
    private final CrewService crewService;
    private final S3Uploader s3Uploader;

    /** 닉네임 사용 가능 여부 - 규칙/비속어 위반은 예외, 중복이면 false */
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        validateNicknameFormat(nickname);
        return !userRepository.existsByNickname(nickname);
    }

    /** 온보딩 프로필 저장 - 닉네임 재검증 후 저장, onboarding_status를 DOG_REQUIRED로 전환 */
    @Transactional
    public void saveProfile(Long userId, UserDto.ProfileRequest request) {
        User user = findUser(userId);
        validateNickname(request.nickname());
        user.completeProfile(request.nickname(), request.height(), request.gender(), request.weight());
    }

    /** 내 정보 조회 */
    @Transactional(readOnly = true)
    public UserDto.MeResponse getMe(Long userId) {
        return UserDto.MeResponse.from(findUser(userId));
    }

    /** 내 정보 수정 (닉네임/키/몸무게, null은 미변경). 닉네임 변경 시 재검증 */
    @Transactional
    public UserDto.MeResponse updateMe(Long userId, UserDto.UpdateRequest request) {
        User user = findUser(userId);
        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            validateNickname(request.nickname());
        }
        user.updateInfo(request.nickname(), request.height(), request.weight());
        return UserDto.MeResponse.from(user);
    }

    /** 비밀번호 변경 - 자체 가입 유저만, 현재 비밀번호 확인 후 규칙 검증 */
    @Transactional
    public void changePassword(Long userId, UserDto.ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!user.isEmailProvider()) {
            throw new BusinessException(ErrorCode.USER_008);
        }
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_009);
        }
        PasswordValidator.validate(request.newPassword(), request.newPasswordConfirm());
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // 비밀번호 변경 후 기존 refresh 무효화 (재로그인 유도)
        tokenService.deleteRefreshToken(userId);
    }

    /**
     * 회원 탈퇴 - 소프트 삭제. deleted_at 기록 + 개인정보 즉시 익명화 + refresh 삭제.
     * payment/coin_transaction/running_record는 보존한다.
     */
    @Transactional
    public void withdraw(Long userId) {
        User user = findUser(userId);
        user.withdraw();
        tokenService.deleteRefreshToken(userId);
        // 상호작용 데이터 삭제: 친구 관계/요청 + 놀이터 초대 (6단계 연결 완료)
        friendService.deleteAllInteractionsOf(userId);
        // 크루 처리: 크루장+크루원 존재 시 위임 필요 에러, 크루장 혼자면 해체, 일반 크루원은 멤버십/신청 삭제 (7단계 연결 완료)
        crewService.handleUserWithdrawal(userId);
        // S3 route/{userId}/ 이미지 일괄 삭제 - 커밋 후 수행, 실패해도 흐름 계속 (9단계 연결 완료, 문서 8.4)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3Uploader.deleteAll("route/" + userId + "/");
            }
        });
    }

    /** 유저 조회 공통 (탈퇴 유저는 @SQLRestriction으로 자동 제외) */
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
    }

    /** 닉네임 형식 + 비속어 + 중복 전체 검증 */
    private void validateNickname(String nickname) {
        validateNicknameFormat(nickname);
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.USER_002);
        }
    }

    /** 닉네임 형식(2~7자, 한글/영문/숫자)과 비속어 검증 */
    private void validateNicknameFormat(String nickname) {
        if (nickname == null || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new BusinessException(ErrorCode.USER_005);
        }
        if (profanityFilter.containsProfanity(nickname)) {
            throw new BusinessException(ErrorCode.USER_006);
        }
    }
}

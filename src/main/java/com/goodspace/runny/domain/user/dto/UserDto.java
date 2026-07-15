package com.goodspace.runny.domain.user.dto;

import com.goodspace.runny.domain.user.entity.Gender;
import com.goodspace.runny.domain.user.entity.OnboardingStatus;
import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 회원 도메인 요청/응답 DTO 모음.
 */
public final class UserDto {

    private UserDto() {
    }

    /** 온보딩 프로필 저장 요청 (닉네임/키/성별/몸무게) */
    public record ProfileRequest(
            @NotBlank String nickname,
            @NotNull @Positive Integer height,
            @NotNull Gender gender,
            @NotNull @Positive Integer weight
    ) {
    }

    /** 내 정보 수정 요청 - null 필드는 미변경 */
    public record UpdateRequest(
            String nickname,
            @Positive Integer height,
            @Positive Integer weight
    ) {
    }

    /** 비밀번호 변경 요청 (자체 가입 유저 전용) */
    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword,
            @NotBlank String newPasswordConfirm
    ) {
    }

    /** 내 정보 조회 응답 */
    public record MeResponse(
            Long id,
            String email,
            String nickname,
            Integer height,
            Integer weight,
            Gender gender,
            int coin,
            Provider provider,
            OnboardingStatus onboardingStatus,
            boolean marketingAgreed
    ) {
        public static MeResponse from(User user) {
            return new MeResponse(
                    user.getId(), user.getEmail(), user.getNickname(),
                    user.getHeight(), user.getWeight(), user.getGender(),
                    user.getCoin(), user.getProvider(), user.getOnboardingStatus(),
                    user.isMarketingAgreed()
            );
        }
    }
}

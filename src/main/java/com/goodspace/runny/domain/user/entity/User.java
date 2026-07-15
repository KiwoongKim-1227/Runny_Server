package com.goodspace.runny.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 회원 엔티티. 소프트 삭제(deleted_at) 방식이며 전 조회에 deleted_at IS NULL이 자동 적용된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname"),
        @UniqueConstraint(name = "uk_users_provider", columnNames = {"provider", "provider_id"})
})
public class User {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    // 소셜 가입 유저는 비밀번호 없음
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(length = 30)
    private String nickname;

    private Integer height;

    private Integer weight;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Gender gender;

    @Column(nullable = false)
    private int coin;

    // 활성 강아지 참조 (user_dog는 3단계 구현 - 지금은 ID 컬럼만 보유)
    @Column(name = "active_dog_id")
    private Long activeDogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    @Column(name = "terms_agreed_at", nullable = false)
    private LocalDateTime termsAgreedAt;

    @Column(name = "marketing_agreed", nullable = false)
    private boolean marketingAgreed;

    @Column(name = "marketing_agreed_at")
    private LocalDateTime marketingAgreedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String email, String password, Provider provider, String providerId,
                 boolean marketingAgreed) {
        LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.coin = 0;
        this.onboardingStatus = OnboardingStatus.PROFILE_REQUIRED;
        this.termsAgreedAt = now;
        this.marketingAgreed = marketingAgreed;
        this.marketingAgreedAt = marketingAgreed ? now : null;
        this.createdAt = now;
    }

    /** 온보딩 프로필 저장 - 닉네임/키/성별/몸무게 설정 후 상태를 DOG_REQUIRED로 전환 */
    public void completeProfile(String nickname, int height, Gender gender, int weight) {
        this.nickname = nickname;
        this.height = height;
        this.gender = gender;
        this.weight = weight;
        if (this.onboardingStatus == OnboardingStatus.PROFILE_REQUIRED) {
            this.onboardingStatus = OnboardingStatus.DOG_REQUIRED;
        }
    }

    /** 내 정보 수정 - 전달된 값만 반영 (null은 미변경) */
    public void updateInfo(String nickname, Integer height, Integer weight) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (height != null) {
            this.height = height;
        }
        if (weight != null) {
            this.weight = weight;
        }
    }

    /** 비밀번호 변경 (BCrypt 해시값 저장) */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 회원 탈퇴 - deleted_at 기록 + 개인정보 즉시 익명화 (deleted_{id} 치환) */
    public void withdraw() {
        String anonymized = "deleted_" + this.id;
        this.deletedAt = LocalDateTime.now(ZONE_SEOUL);
        this.email = anonymized;
        this.nickname = anonymized;
        this.providerId = anonymized;
        this.password = null;
    }

    /** 자체(이메일) 가입 유저 여부 - 비밀번호 변경/찾기 가능 조건 */
    public boolean isEmailProvider() {
        return this.provider == Provider.EMAIL;
    }
}

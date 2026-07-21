package com.goodspace.runny.domain.achievement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 횟수/연속형 업적의 진행 카운터 엔티티. 야간·모닝 순찰(2km+ 러닝 5회), 전담 사진 작가(꾸미기 30회),
 * 일일 올클리어(10일), 장거리 마스터(10km x3), 철인 훈련(연속 3일) 등의 진행도를 유저x업적코드 단위로 저장한다.
 * last_period_key는 "하루 1회만 카운트"와 "연속 일수" 판정에 사용한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "achievement_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_achievement_progress_user_code", columnNames = {"user_id", "code"})
})
public class AchievementProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private double progress;

    // 마지막 카운트 기준일 (yyyy-MM-dd) - 하루 1회 제한/연속 일수 판정용
    @Column(name = "last_period_key", length = 10)
    private String lastPeriodKey;

    public AchievementProgress(Long userId, String code) {
        this.userId = userId;
        this.code = code;
        this.progress = 0;
    }

    /** 단순 카운트 +1 */
    public void increment() {
        this.progress += 1;
    }

    /** 같은 기간(일) 내 1회만 카운트 - 이미 카운트된 기간이면 false */
    public boolean incrementOncePerPeriod(String periodKey) {
        if (periodKey.equals(this.lastPeriodKey)) {
            return false;
        }
        this.lastPeriodKey = periodKey;
        this.progress += 1;
        return true;
    }

    /** 연속 일수 갱신 - 같은 날 재호출 무시, 어제에 이어지면 +1, 끊겼으면 1로 리셋 */
    public void continueOrResetStreak(String todayKey, String yesterdayKey) {
        if (todayKey.equals(this.lastPeriodKey)) {
            return;
        }
        this.progress = yesterdayKey.equals(this.lastPeriodKey) ? this.progress + 1 : 1;
        this.lastPeriodKey = todayKey;
    }
}

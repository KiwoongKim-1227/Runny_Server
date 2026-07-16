package com.goodspace.runny.domain.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저별 퀘스트 진행 엔티티. period_key(일: yyyy-MM-dd, 주: yyyy-Www) 기반으로
 * 스케줄러 없이 조회 시점 lazy 생성으로 일일/주간 초기화를 처리한다.
 * target_value는 출제 시점의 랜덤 확정치 스냅샷이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_quest", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_quest_period", columnNames = {"user_id", "quest_id", "period_key"})
})
public class UserQuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(name = "period_key", nullable = false, length = 10)
    private String periodKey;

    @Column(name = "target_value", nullable = false)
    private double targetValue;

    @Column(nullable = false)
    private double progress;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean claimed;

    public UserQuest(Long userId, Quest quest, String periodKey, double targetValue) {
        this.userId = userId;
        this.quest = quest;
        this.periodKey = periodKey;
        this.targetValue = targetValue;
        this.progress = 0;
        this.completed = false;
        this.claimed = false;
    }

    /** 최대치 갱신 방식 진행도 (1회 러닝 거리/시간/무정지 - 더 좋은 기록으로 갱신) */
    public void updateMaxProgress(double value) {
        if (claimed) {
            return;
        }
        this.progress = Math.max(this.progress, value);
        checkCompleted();
    }

    /** 누적 방식 진행도 (횟수/주간 누적 거리/꾸미기) */
    public void addProgress(double delta) {
        if (claimed) {
            return;
        }
        this.progress += delta;
        checkCompleted();
    }

    /** 즉시 달성 처리 (접속하기) */
    public void completeNow() {
        if (claimed) {
            return;
        }
        this.progress = Math.max(this.progress, this.targetValue);
        this.completed = true;
    }

    /** 보상 수령 처리 */
    public void claim() {
        this.claimed = true;
    }

    private void checkCompleted() {
        if (this.progress >= this.targetValue) {
            this.completed = true;
        }
    }
}

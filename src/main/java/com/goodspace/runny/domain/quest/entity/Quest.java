package com.goodspace.runny.domain.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 퀘스트 마스터 엔티티. 수치형 조건은 min~max 범위를 두고 일일 랜덤 출제 시 범위 내 랜덤 값으로 확정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestType type;

    @Column(nullable = false, length = 50)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 20)
    private QuestConditionType conditionType;

    // 조건 수치 범위 (고정 수치 퀘스트는 min == max). 단위: 거리 km, 시간 분, 횟수 회
    @Column(name = "min_value", nullable = false)
    private double minValue;

    @Column(name = "max_value", nullable = false)
    private double maxValue;

    @Column(name = "reward_exp", nullable = false)
    private int rewardExp;

    @Column(name = "reward_coin", nullable = false)
    private int rewardCoin;

    public Quest(QuestType type, String title, QuestConditionType conditionType,
                 double minValue, double maxValue, int rewardExp, int rewardCoin) {
        this.type = type;
        this.title = title;
        this.conditionType = conditionType;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.rewardExp = rewardExp;
        this.rewardCoin = rewardCoin;
    }
}

package com.goodspace.runny.domain.achievement.entity;

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

/**
 * 업적 마스터 엔티티. code는 견종 해금 매칭과 판정 로직 식별에 사용하며 아이콘(image_url)을 포함한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "achievement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_achievement_code", columnNames = "code")
})
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 200)
    private String description;

    // 업적 아이콘 (S3 정적 리소스)
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 20)
    private RewardType rewardType;

    @Column(name = "reward_coin", nullable = false)
    private int rewardCoin;

    // 견종 해금형 보상의 대상 견종 (COIN형은 null)
    @Column(name = "reward_breed_id")
    private Long rewardBreedId;

    @Builder
    private Achievement(String code, String title, String description, String imageUrl,
                        RewardType rewardType, int rewardCoin, Long rewardBreedId) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.rewardType = rewardType;
        this.rewardCoin = rewardCoin;
        this.rewardBreedId = rewardBreedId;
    }
}

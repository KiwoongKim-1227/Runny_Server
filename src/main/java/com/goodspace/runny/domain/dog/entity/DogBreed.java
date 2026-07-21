package com.goodspace.runny.domain.dog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 견종 마스터 엔티티. 일반 8종 + 레어 4종(업적 해금 2, 고가 구매 2)을 시드로 관리한다.
 * 외형은 3D 모델(glb)로 제공한다. 업적 해금형은 unlock_achievement_code로 식별하며, FK(unlock_achievement_id)는 8단계 업적 시드 후 연결한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "dog_breed")
public class DogBreed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    // 온보딩/입양 캐러셀에 노출할 소개 문구
    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BreedGrade grade;

    // 입양 가격 (업적 해금형은 해금 후 무료이므로 0)
    @Column(nullable = false)
    private int price;

    // 업적 해금형 견종의 업적 코드 (예: BORDER_COLLIE). null이면 코인 구매형
    @Column(name = "unlock_achievement_code", length = 40)
    private String unlockAchievementCode;

    // 업적 마스터 FK - 8단계 업적 시드 등록 후 코드 매칭으로 백필 예정
    @Column(name = "unlock_achievement_id")
    private Long unlockAchievementId;

    // 견종 3D 모델(glb) URL - id 확정 후 시더가 컨벤션(breed/{id}.glb)으로 할당
    @Column(name = "model_url", length = 500)
    private String modelUrl;

    @Builder
    private DogBreed(String name, String description, BreedGrade grade, int price,
                     String unlockAchievementCode) {
        this.name = name;
        this.description = description;
        this.grade = grade;
        this.price = price;
        this.unlockAchievementCode = unlockAchievementCode;
    }

    /** 모델 URL 할당 - IDENTITY 전략상 id 확정(insert) 후 시더가 호출 */
    public void assignModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }

    /** 업적 해금이 필요한 견종 여부 */
    public boolean requiresAchievementUnlock() {
        return this.unlockAchievementCode != null;
    }

    /** 업적 마스터 FK 백필 - 8단계 업적 시드 등록 후 코드 매칭으로 연결 */
    public void linkUnlockAchievement(Long achievementId) {
        this.unlockAchievementId = achievementId;
    }
}

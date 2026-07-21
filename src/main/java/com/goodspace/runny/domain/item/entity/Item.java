package com.goodspace.runny.domain.item.entity;

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
 * 아이템 마스터 엔티티. 6개 카테고리, 등급별 가격 분포, 외형은 S3 정적 리소스 3D 모델(glb, item/ 프리픽스).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemCategory category;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemTier tier;

    @Column(nullable = false)
    private int price;

    // 아이템 3D 모델(glb) URL - id 확정 후 시더가 컨벤션(item/{id}.glb)으로 할당
    @Column(name = "model_url", length = 500)
    private String modelUrl;

    public Item(ItemCategory category, String name, ItemTier tier, int price) {
        this.category = category;
        this.name = name;
        this.tier = tier;
        this.price = price;
    }

    /** 모델 URL 할당 - IDENTITY 전략상 id 확정(insert) 후 시더가 호출 */
    public void assignModelUrl(String modelUrl) {
        this.modelUrl = modelUrl;
    }
}

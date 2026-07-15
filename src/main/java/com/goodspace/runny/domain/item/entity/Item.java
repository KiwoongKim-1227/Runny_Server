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
 * 아이템 마스터 엔티티. 6개 카테고리, 등급별 가격 분포, 이미지는 S3 정적 리소스(item/ 프리픽스).
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

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    public Item(ItemCategory category, String name, ItemTier tier, int price, String imageUrl) {
        this.category = category;
        this.name = name;
        this.tier = tier;
        this.price = price;
        this.imageUrl = imageUrl;
    }
}

package com.goodspace.runny.domain.running.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 히스토리 꾸미기 스티커 마스터 엔티티. 편집 결과물은 서버에 저장하지 않는다(기기 저장/SNS 공유).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sticker")
public class Sticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    public Sticker(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }
}

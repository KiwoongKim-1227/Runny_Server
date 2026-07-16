package com.goodspace.runny.domain.achievement.entity;

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
 * 랜드마크 마스터 엔티티. 동네 대장 도장 깨기 업적용 - 방문 판정(좌표 반경 체크)은 프론트가 수행하고
 * 백엔드는 방문 ID 목록을 수신해 기록한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "landmark")
public class Landmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "radius_m", nullable = false)
    private int radiusM;

    public Landmark(String name, double latitude, double longitude, int radiusM) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusM = radiusM;
    }
}

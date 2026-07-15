package com.goodspace.runny.domain.crew.entity;

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
 * 크루 엔티티. 크루명 UNIQUE(최대 8자), 로고 미설정 시 기본 이미지 상수 사용,
 * 정원 기본 50명(1,000코인당 +50 확장), total_distance는 크루원 러닝 완료 시 가산(9단계).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "crew", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crew_name", columnNames = "name")
})
public class Crew {

    public static final int DEFAULT_MAX_MEMBERS = 50;
    public static final int CAPACITY_EXPAND_UNIT = 50;
    // 로고 미설정 시 사용하는 기본 이미지 (운영자 등록 정적 리소스)
    public static final String DEFAULT_IMAGE_URL =
            "https://runny-assets.s3.ap-northeast-2.amazonaws.com/crew/default.png";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String name;

    // null 허용 - 응답 시 기본 이미지로 대체
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 30)
    private String intro;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    // 크루 총 누적 거리 (km)
    @Column(name = "total_distance", nullable = false)
    private double totalDistance;

    public Crew(String name, String imageUrl, String intro, Long leaderId) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.intro = intro;
        this.leaderId = leaderId;
        this.maxMembers = DEFAULT_MAX_MEMBERS;
        this.totalDistance = 0;
    }

    /** 표시용 이미지 URL - 미설정 시 기본 이미지 */
    public String displayImageUrl() {
        return imageUrl != null ? imageUrl : DEFAULT_IMAGE_URL;
    }

    /** 크루명 변경 (검증은 서비스에서 수행) */
    public void changeName(String name) {
        this.name = name;
    }

    /** 한줄소개 변경 (무료) */
    public void changeIntro(String intro) {
        this.intro = intro;
    }

    /** 로고 이미지 교체 */
    public void changeImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /** 정원 확장 - 1,000코인당 +50 */
    public void expandCapacity() {
        this.maxMembers += CAPACITY_EXPAND_UNIT;
    }

    /** 크루장 위임 */
    public void changeLeader(Long newLeaderId) {
        this.leaderId = newLeaderId;
    }

    /** 러닝 완료 시 누적 거리 가산 (9단계에서 호출) */
    public void addDistance(double distanceKm) {
        this.totalDistance += distanceKm;
    }
}

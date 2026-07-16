package com.goodspace.runny.domain.running.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 러닝 기록 엔티티 (문서 6.1). client_run_id UNIQUE 멱등키로 오프라인 재동기화 중복 저장을 방지하고,
 * 당시 강아지 이름/외형 스냅샷을 저장해 이후 종/코디 변경과 무관하게 동일한 모습으로 리포트를 재조회한다.
 * 회원 탈퇴 후에도 보존된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "running_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_running_client_run_id", columnNames = "client_run_id")
})
public class RunningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_dog_id", nullable = false)
    private Long userDogId;

    // 클라이언트 생성 멱등키 - 오프라인 재동기화 시 중복 저장 방지
    @Column(name = "client_run_id", nullable = false, length = 64)
    private String clientRunId;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @Column(name = "duration_sec", nullable = false)
    private long durationSec;

    @Column(name = "avg_pace_sec", nullable = false)
    private long avgPaceSec;

    // 서버 계산 최종값: 1.036 x 체중(kg) x 거리(km)
    @Column(nullable = false)
    private int calories;

    @Column(nullable = false)
    private int cadence;

    // 웨어러블 없으면 null 저장, 조회 시 프론트가 "-" 표시
    @Column(name = "avg_heart_rate")
    private Integer avgHeartRate;

    @Column(name = "longest_nonstop_sec", nullable = false)
    private long longestNonstopSec;

    @Column(name = "elevation_m", nullable = false)
    private double elevationM;

    // 지도 포함 경로 캡처 (리포트/히스토리 목록용)
    @Column(name = "route_image_url", length = 500)
    private String routeImageUrl;

    // 투명 배경 경로 선 이미지 (히스토리 꾸미기용)
    @Column(name = "route_line_image_url", length = 500)
    private String routeLineImageUrl;

    // "{강아지이름}와 함께한 {요일} {시간대} 러닝"
    @Column(nullable = false, length = 60)
    private String title;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "stamina_delta", nullable = false)
    private int staminaDelta;

    @Column(name = "endurance_delta", nullable = false)
    private int enduranceDelta;

    @Column(name = "speed_delta", nullable = false)
    private int speedDelta;

    @Column(name = "dog_name_snapshot", nullable = false, length = 7)
    private String dogNameSnapshot;

    // 당시 강아지 외형(견종 이미지 + 착용 아이템) JSON 스냅샷
    @Column(name = "dog_appearance_snapshot", columnDefinition = "TEXT")
    private String dogAppearanceSnapshot;

    // 미확인 리포트 여부 (히스토리 빨간 점) - 상세 조회 시 읽음 처리
    @Column(name = "is_checked", nullable = false)
    private boolean checked;

    @Builder
    private RunningRecord(Long userId, Long userDogId, String clientRunId, double distanceKm,
                          long durationSec, long avgPaceSec, int calories, int cadence,
                          Integer avgHeartRate, long longestNonstopSec, double elevationM,
                          String routeImageUrl, String routeLineImageUrl, String title,
                          LocalDateTime startedAt, LocalDateTime endedAt,
                          int staminaDelta, int enduranceDelta, int speedDelta,
                          String dogNameSnapshot, String dogAppearanceSnapshot) {
        this.userId = userId;
        this.userDogId = userDogId;
        this.clientRunId = clientRunId;
        this.distanceKm = distanceKm;
        this.durationSec = durationSec;
        this.avgPaceSec = avgPaceSec;
        this.calories = calories;
        this.cadence = cadence;
        this.avgHeartRate = avgHeartRate;
        this.longestNonstopSec = longestNonstopSec;
        this.elevationM = elevationM;
        this.routeImageUrl = routeImageUrl;
        this.routeLineImageUrl = routeLineImageUrl;
        this.title = title;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.staminaDelta = staminaDelta;
        this.enduranceDelta = enduranceDelta;
        this.speedDelta = speedDelta;
        this.dogNameSnapshot = dogNameSnapshot;
        this.dogAppearanceSnapshot = dogAppearanceSnapshot;
        this.checked = false;
    }

    /** 리포트 상세 조회 시 읽음 처리 */
    public void markChecked() {
        this.checked = true;
    }
}

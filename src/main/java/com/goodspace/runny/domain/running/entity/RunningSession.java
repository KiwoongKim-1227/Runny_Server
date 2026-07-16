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

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 러닝 세션 시작 기록 엔티티 (선택적). 시작 시 네트워크 오류면 프론트가 로컬로 진행하고
 * 종료 API만으로 기록이 생성되므로, 세션은 시작 시각/사용 강아지 참고 기록 용도다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "running_session")
public class RunningSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 시작 시점 사용 강아지 스냅샷
    @Column(name = "user_dog_id", nullable = false)
    private Long userDogId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    public RunningSession(Long userId, Long userDogId) {
        this.userId = userId;
        this.userDogId = userDogId;
        this.startedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}

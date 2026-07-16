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
 * 구간별(1km) 페이스 엔티티. km_index는 1부터 시작한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "running_split")
public class RunningSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "running_record_id", nullable = false)
    private Long runningRecordId;

    @Column(name = "km_index", nullable = false)
    private int kmIndex;

    @Column(name = "pace_sec", nullable = false)
    private int paceSec;

    public RunningSplit(Long runningRecordId, int kmIndex, int paceSec) {
        this.runningRecordId = runningRecordId;
        this.kmIndex = kmIndex;
        this.paceSec = paceSec;
    }
}

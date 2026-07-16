package com.goodspace.runny.domain.running.repository;

import com.goodspace.runny.domain.running.entity.RunningSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 구간별 페이스 리포지토리.
 */
public interface RunningSplitRepository extends JpaRepository<RunningSplit, Long> {

    List<RunningSplit> findByRunningRecordIdOrderByKmIndexAsc(Long runningRecordId);
}

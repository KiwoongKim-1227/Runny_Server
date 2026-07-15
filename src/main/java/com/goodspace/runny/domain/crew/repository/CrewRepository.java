package com.goodspace.runny.domain.crew.repository;

import com.goodspace.runny.domain.crew.entity.Crew;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 크루 리포지토리.
 */
public interface CrewRepository extends JpaRepository<Crew, Long> {

    boolean existsByName(String name);

    Optional<Crew> findByName(String name);

    /** 크루명 부분 일치 검색 */
    Page<Crew> findByNameContaining(String name, Pageable pageable);
}

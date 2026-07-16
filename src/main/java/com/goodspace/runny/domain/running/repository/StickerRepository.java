package com.goodspace.runny.domain.running.repository;

import com.goodspace.runny.domain.running.entity.Sticker;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스티커 마스터 리포지토리.
 */
public interface StickerRepository extends JpaRepository<Sticker, Long> {
}

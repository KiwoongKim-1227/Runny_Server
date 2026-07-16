package com.goodspace.runny.domain.coin.repository;

import com.goodspace.runny.domain.coin.entity.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 코인 원장 리포지토리. 내역 조회는 전체 반환한다 (MVP 규모에서 페이징 제거).
 */
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    List<CoinTransaction> findByUserIdOrderByIdDesc(Long userId);
}

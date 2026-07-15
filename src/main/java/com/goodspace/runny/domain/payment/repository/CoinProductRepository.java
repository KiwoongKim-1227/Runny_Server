package com.goodspace.runny.domain.payment.repository;

import com.goodspace.runny.domain.payment.entity.CoinProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 코인 상품 마스터 리포지토리.
 */
public interface CoinProductRepository extends JpaRepository<CoinProduct, Long> {

    List<CoinProduct> findAllByOrderByPriceAsc();
}

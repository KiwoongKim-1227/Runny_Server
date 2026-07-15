package com.goodspace.runny.domain.payment.service;

import com.goodspace.runny.domain.payment.entity.CoinProduct;
import com.goodspace.runny.domain.payment.repository.CoinProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 코인 상품 시드 데이터 등록기. 기획 문서 4.E의 5종을 최초 기동 시 등록한다.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class CoinProductSeeder implements CommandLineRunner {

    private final CoinProductRepository coinProductRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (coinProductRepository.count() > 0) {
            return;
        }
        coinProductRepository.saveAll(List.of(
                new CoinProduct(500, 1_900),
                new CoinProduct(1_200, 3_900),
                new CoinProduct(2_600, 7_900),
                new CoinProduct(5_500, 14_900),
                new CoinProduct(12_000, 29_900)
        ));
        log.info("코인 상품 시드 5종 등록 완료");
    }
}

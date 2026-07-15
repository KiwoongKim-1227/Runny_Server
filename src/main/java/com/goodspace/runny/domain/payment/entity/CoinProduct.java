package com.goodspace.runny.domain.payment.entity;

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
 * 코인 상품 마스터 엔티티. NicePay 인앱 결제로 판매하는 코인 5종을 시드로 관리한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coin_product")
public class CoinProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 지급 코인 수량
    @Column(name = "coin_amount", nullable = false)
    private int coinAmount;

    // 판매 가격 (원)
    @Column(nullable = false)
    private int price;

    public CoinProduct(int coinAmount, int price) {
        this.coinAmount = coinAmount;
        this.price = price;
    }
}

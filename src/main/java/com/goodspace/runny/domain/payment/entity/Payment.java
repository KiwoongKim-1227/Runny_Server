package com.goodspace.runny.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 결제 엔티티. 주문 생성 시 PENDING으로 저장되고 승인 검증 성공 시 COMPLETED로 전이된다.
 * order_id UNIQUE + 상태 확인으로 중복 승인을 멱등 처리한다. 회원 탈퇴 후에도 보존된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_order_id", columnNames = "order_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_product_id", nullable = false)
    private CoinProduct coinProduct;

    // 주문 금액 (원) - NicePay 승인 응답 금액과 대조해 위변조를 방지한다
    @Column(nullable = false)
    private int amount;

    // NicePay 거래 ID (승인 요청 시 저장)
    @Column(length = 64)
    private String tid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Payment(Long userId, String orderId, CoinProduct coinProduct) {
        this.userId = userId;
        this.orderId = orderId;
        this.coinProduct = coinProduct;
        this.amount = coinProduct.getPrice();
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /** 승인 처리 시 거래 ID 기록 */
    public void assignTid(String tid) {
        this.tid = tid;
    }
}

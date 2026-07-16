package com.goodspace.runny.domain.payment.entity;

/**
 * 결제 상태. PROCESSING은 승인 처리 선점 상태로, PENDING -> PROCESSING 조건부 전이에
 * 성공한 요청만 NicePay 승인 API를 호출할 수 있다 (외부 PG 중복 호출 방지).
 */
public enum PaymentStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}

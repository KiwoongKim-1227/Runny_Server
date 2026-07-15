package com.goodspace.runny.domain.payment.entity;

/**
 * 결제 상태. 동일 orderId 재승인 요청은 이 상태 확인으로 멱등 처리한다.
 */
public enum PaymentStatus {
    PENDING, COMPLETED, FAILED, CANCELLED
}

package com.goodspace.runny.domain.payment.dto;

import com.goodspace.runny.domain.payment.entity.CoinProduct;
import com.goodspace.runny.domain.payment.entity.Payment;
import com.goodspace.runny.domain.payment.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 도메인 요청/응답 DTO 모음.
 */
public final class PaymentDto {

    private PaymentDto() {
    }

    /** 코인 상품 응답 */
    public record CoinProductResponse(
            Long productId,
            int coinAmount,
            int price
    ) {
        public static CoinProductResponse from(CoinProduct product) {
            return new CoinProductResponse(product.getId(), product.getCoinAmount(), product.getPrice());
        }
    }

    /** 주문 생성 요청 */
    public record OrderCreateRequest(
            @NotNull Long coinProductId
    ) {
    }

    /** 주문 생성 응답 - 프론트가 이 orderId/amount로 NicePay 결제창을 진행한다 */
    public record OrderCreateResponse(
            String orderId,
            int amount,
            int coinAmount
    ) {
        public static OrderCreateResponse from(Payment payment) {
            return new OrderCreateResponse(payment.getOrderId(), payment.getAmount(),
                    payment.getCoinProduct().getCoinAmount());
        }
    }

    /** 승인 요청 - 결제창 완료 후 프론트가 전달 */
    public record ApproveRequest(
            @NotBlank String orderId,
            @NotBlank String tid
    ) {
    }

    /** 승인 응답 - 멱등 처리 시에도 동일 형태로 반환 */
    public record ApproveResponse(
            String orderId,
            PaymentStatus status,
            int coinGranted,
            int amount
    ) {
        public static ApproveResponse of(Payment payment, int coinGranted) {
            return new ApproveResponse(payment.getOrderId(), payment.getStatus(), coinGranted, payment.getAmount());
        }
    }
}

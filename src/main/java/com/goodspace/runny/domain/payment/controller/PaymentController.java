package com.goodspace.runny.domain.payment.controller;

import com.goodspace.runny.domain.payment.dto.PaymentDto;
import com.goodspace.runny.domain.payment.service.PaymentService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API 컨트롤러. NicePay 주문 생성과 승인 검증을 제공한다.
 */
@Tag(name = "Payment", description = "NicePay 결제 API - 주문 생성, 승인 검증 + 코인 지급")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** 주문 생성 - 프론트는 응답의 orderId/amount로 NicePay 결제창(SDK/웹뷰)을 진행한다 */
    @Operation(summary = "NicePay 주문 생성",
            description = "서버가 orderId를 발급하고 상품/금액을 PENDING으로 저장한다. 존재하지 않는 상품 PAYMENT_005")
    @PostMapping("/orders")
    public ApiResponse<PaymentDto.OrderCreateResponse> createOrder(
            @Valid @RequestBody PaymentDto.OrderCreateRequest request) {
        return ApiResponse.ok(paymentService.createOrder(SecurityUtil.currentUserId(), request));
    }

    /** 승인 검증 - 결제창 완료 후 tid/orderId 전달 */
    @Operation(summary = "NicePay 승인 검증 + 코인 지급",
            description = "서버가 NicePay 승인 API를 호출해 금액 일치를 검증한다. "
                    + "금액 불일치 PAYMENT_001(취소 후 FAILED), 승인 실패 PAYMENT_003, "
                    + "동일 orderId 재승인은 멱등 처리(COMPLETED면 기존 결과 반환), 실패/취소 주문 재승인 PAYMENT_004")
    @PostMapping("/approve")
    public ApiResponse<PaymentDto.ApproveResponse> approve(
            @Valid @RequestBody PaymentDto.ApproveRequest request) {
        return ApiResponse.ok(paymentService.approve(SecurityUtil.currentUserId(), request));
    }
}

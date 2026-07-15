package com.goodspace.runny.domain.payment.service;

import com.goodspace.runny.domain.payment.client.NicePayClient;
import com.goodspace.runny.domain.payment.dto.PaymentDto;
import com.goodspace.runny.domain.payment.entity.CoinProduct;
import com.goodspace.runny.domain.payment.entity.Payment;
import com.goodspace.runny.domain.payment.entity.PaymentStatus;
import com.goodspace.runny.domain.payment.repository.CoinProductRepository;
import com.goodspace.runny.domain.payment.repository.PaymentRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 결제 오케스트레이션 서비스. 주문 생성과 승인 검증 흐름을 담당한다.
 * 승인 흐름: NicePay 호출(트랜잭션 밖) -> 금액 일치 검증 -> 상태 전이 + 코인 지급(트랜잭션).
 * 동일 orderId 재승인은 status 확인으로 멱등 처리한다(COMPLETED면 기존 결과 반환).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CoinProductRepository coinProductRepository;
    private final PaymentTransactionService paymentTransactionService;
    private final NicePayClient nicePayClient;

    /** 코인 상품 5종 조회 */
    @Transactional(readOnly = true)
    public List<PaymentDto.CoinProductResponse> getCoinProducts() {
        return coinProductRepository.findAllByOrderByPriceAsc().stream()
                .map(PaymentDto.CoinProductResponse::from)
                .toList();
    }

    /** 주문 생성 - 서버가 orderId를 발급하고 상품/금액을 PENDING으로 저장 (금액 위변조 방지의 기준값) */
    @Transactional
    public PaymentDto.OrderCreateResponse createOrder(Long userId, PaymentDto.OrderCreateRequest request) {
        CoinProduct product = coinProductRepository.findById(request.coinProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_005));
        String orderId = "RUNNY-" + UUID.randomUUID();
        Payment payment = paymentRepository.save(new Payment(userId, orderId, product));
        return PaymentDto.OrderCreateResponse.from(payment);
    }

    /**
     * 승인 검증 - tid/orderId 수신 후 NicePay 승인 API 호출, 응답 금액과 주문 금액 일치 검증.
     * 성공: COMPLETED + 코인 지급. 실패/불일치: 취소 API 호출 후 FAILED.
     */
    public PaymentDto.ApproveResponse approve(Long userId, PaymentDto.ApproveRequest request) {
        Payment payment = paymentRepository.findByOrderIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_002));

        // 멱등 처리: 이미 완료된 주문은 기존 결과 반환, 실패/취소 주문은 재승인 불가
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentDto.ApproveResponse.of(payment, payment.getCoinProduct().getCoinAmount());
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_004);
        }

        // NicePay 승인 호출은 트랜잭션 밖에서 선행 (문서 8.5)
        NicePayClient.ApproveResult result = nicePayClient.approve(request.tid(), payment.getAmount());

        if (!result.success()) {
            log.warn("NicePay 승인 실패: orderId={}, resultCode={}, msg={}",
                    request.orderId(), result.resultCode(), result.resultMsg());
            nicePayClient.cancel(request.tid(), request.orderId(), "승인 실패 처리");
            paymentTransactionService.fail(request.orderId(), request.tid());
            throw new BusinessException(ErrorCode.PAYMENT_003);
        }
        if (result.amount() != payment.getAmount()) {
            log.warn("NicePay 금액 불일치: orderId={}, 주문={}, 응답={}",
                    request.orderId(), payment.getAmount(), result.amount());
            nicePayClient.cancel(request.tid(), request.orderId(), "금액 불일치 취소");
            paymentTransactionService.fail(request.orderId(), request.tid());
            throw new BusinessException(ErrorCode.PAYMENT_001);
        }

        // 상태 전이 + 코인 지급. 동시 요청으로 전이에 실패했다면 처리된 최신 상태를 반환(멱등)
        paymentTransactionService.complete(request.orderId(), request.tid());
        Payment completed = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_002));
        return PaymentDto.ApproveResponse.of(completed, completed.getCoinProduct().getCoinAmount());
    }

    // TODO(MVP 범위 외 확장 지점): NicePay Webhook 수신으로 클라이언트 미도달 승인 건 동기화
    // TODO(MVP 범위 외 확장 지점): 결제 취소/부분 환불 - 코인 회수 정책과 함께 설계 필요
}

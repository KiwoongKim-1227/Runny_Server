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
 * 승인 흐름: PENDING -> PROCESSING 선점(조건부 UPDATE) -> 선점 성공 요청만 NicePay 호출(트랜잭션 밖)
 * -> 금액/주문번호 일치 검증 -> PROCESSING -> COMPLETED 전이 + 코인 지급.
 * 동시 승인 요청은 선점 단계에서 차단되어 외부 PG 중복 호출과 중복 지급이 모두 방지된다.
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
     * 승인 검증 - 멱등/동시성 처리 후 NicePay 승인 API 호출, 응답의 금액과 주문번호 일치를 모두 검증한다.
     * 성공: COMPLETED + 코인 지급. 실패/불일치/예외: 취소 API 호출 후 FAILED.
     */
    public PaymentDto.ApproveResponse approve(Long userId, PaymentDto.ApproveRequest request) {
        Payment payment = paymentRepository.findByOrderIdAndUserId(request.orderId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_002));

        // 멱등 처리: 완료된 주문은 기존 결과 반환, 실패/취소 주문은 재승인 불가
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return PaymentDto.ApproveResponse.of(payment, payment.getCoinProduct().getCoinAmount());
        }
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.PAYMENT_004);
        }

        // 동시성 선점: PENDING -> PROCESSING 전이 실패 = 다른 요청이 처리 중이거나 이미 처리됨
        if (!paymentTransactionService.startProcessing(request.orderId())) {
            Payment current = paymentRepository.findByOrderId(request.orderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_002));
            if (current.getStatus() == PaymentStatus.COMPLETED) {
                return PaymentDto.ApproveResponse.of(current, current.getCoinProduct().getCoinAmount());
            }
            if (current.getStatus() == PaymentStatus.PROCESSING) {
                throw new BusinessException(ErrorCode.PAYMENT_006);
            }
            throw new BusinessException(ErrorCode.PAYMENT_004);
        }

        // 선점 성공 요청만 NicePay 승인 호출 (트랜잭션 밖, 문서 8.5). 예외 시에도 FAILED 정리 후 전파
        NicePayClient.ApproveResult result;
        try {
            result = nicePayClient.approve(request.tid(), payment.getAmount());
        } catch (Exception e) {
            paymentTransactionService.fail(request.orderId(), request.tid());
            throw e;
        }

        if (!result.success()) {
            log.warn("NicePay 승인 실패: orderId={}, resultCode={}, msg={}",
                    request.orderId(), result.resultCode(), result.resultMsg());
            nicePayClient.cancel(request.tid(), request.orderId(), "승인 실패 처리");
            paymentTransactionService.fail(request.orderId(), request.tid());
            throw new BusinessException(ErrorCode.PAYMENT_003);
        }
        // 금액 + 주문번호 일치 검증 (tid 바꿔치기로 다른 주문의 승인 결과를 재사용하는 공격 차단)
        if (result.amount() != payment.getAmount()
                || !payment.getOrderId().equals(result.orderId())) {
            log.warn("NicePay 승인 응답 불일치: orderId={}, 주문금액={}, 응답금액={}, 응답orderId={}",
                    request.orderId(), payment.getAmount(), result.amount(), result.orderId());
            nicePayClient.cancel(request.tid(), request.orderId(), "금액/주문번호 불일치 취소");
            paymentTransactionService.fail(request.orderId(), request.tid());
            throw new BusinessException(ErrorCode.PAYMENT_001);
        }

        // 상태 전이 + 코인 지급 후 최신 상태로 응답
        paymentTransactionService.complete(request.orderId(), request.tid());
        Payment completed = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_002));
        return PaymentDto.ApproveResponse.of(completed, completed.getCoinProduct().getCoinAmount());
    }

    // TODO(MVP 범위 외 확장 지점): NicePay Webhook 수신으로 클라이언트 미도달 승인 건 동기화 + PROCESSING 고아 건 정리 배치
    // TODO(MVP 범위 외 확장 지점): 결제 취소/부분 환불 - 코인 회수 정책과 함께 설계 필요
}

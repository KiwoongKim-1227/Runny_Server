package com.goodspace.runny.domain.payment.service;

import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.payment.entity.Payment;
import com.goodspace.runny.domain.payment.entity.PaymentStatus;
import com.goodspace.runny.domain.payment.repository.PaymentRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 전이 전용 서비스. NicePay 호출은 트랜잭션 밖(오케스트레이터)에서 수행하고,
 * DB 변경만 여기서 트랜잭션으로 처리한다 (문서 8.5).
 * PENDING -> PROCESSING 선점에 성공한 요청만 외부 PG 승인을 호출하며,
 * PROCESSING -> COMPLETED 전이 성공 시에만 코인을 지급해 중복 호출/중복 지급을 모두 차단한다.
 */
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final CoinService coinService;

    /**
     * 승인 처리 선점 - PENDING -> PROCESSING 조건부 전이.
     * 별도 트랜잭션(REQUIRES_NEW)으로 즉시 커밋해 다른 요청이 선점 상태를 볼 수 있게 한다.
     * 반환값이 true인 요청만 NicePay 승인 API를 호출할 수 있다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean startProcessing(String orderId) {
        return paymentRepository.transition(orderId, PaymentStatus.PENDING, PaymentStatus.PROCESSING) == 1;
    }

    /** 승인 완료 처리 - PROCESSING -> COMPLETED 전이에 성공한 요청만 코인을 지급한다 */
    @Transactional
    public boolean complete(String orderId, String tid) {
        int affected = paymentRepository.transition(orderId, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED);
        if (affected == 0) {
            return false;
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_002));
        payment.assignTid(tid);
        coinService.add(payment.getUserId(), payment.getCoinProduct().getCoinAmount(),
                CoinTransactionType.CHARGE, payment.getId());
        return true;
    }

    /** 승인 실패/불일치/예외 처리 - PROCESSING -> FAILED 전이 + 거래 ID 기록 */
    @Transactional
    public void fail(String orderId, String tid) {
        int affected = paymentRepository.transition(orderId, PaymentStatus.PROCESSING, PaymentStatus.FAILED);
        if (affected == 1 && tid != null) {
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> payment.assignTid(tid));
        }
    }
}

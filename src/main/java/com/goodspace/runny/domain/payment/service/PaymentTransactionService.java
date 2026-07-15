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
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 전이 전용 서비스. NicePay 호출은 트랜잭션 밖(오케스트레이터)에서 선행하고,
 * DB 변경(상태 전이 + 코인 지급 + 원장)만 여기서 하나의 트랜잭션으로 처리한다 (문서 8.5).
 * PENDING 조건부 UPDATE로 동시 승인 요청에도 코인이 1회만 지급된다.
 */
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final CoinService coinService;

    /** 승인 완료 처리 - 상태 전이에 성공한 요청만 코인을 지급한다. 반환값은 이번 요청의 처리 주체 여부 */
    @Transactional
    public boolean complete(String orderId, String tid) {
        int affected = paymentRepository.transitionFromPending(orderId, PaymentStatus.COMPLETED);
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

    /** 승인 실패/금액 불일치 처리 - FAILED 전이 + 거래 ID 기록 */
    @Transactional
    public void fail(String orderId, String tid) {
        int affected = paymentRepository.transitionFromPending(orderId, PaymentStatus.FAILED);
        if (affected == 1) {
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> payment.assignTid(tid));
        }
    }
}

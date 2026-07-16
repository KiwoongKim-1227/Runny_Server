package com.goodspace.runny.domain.payment.repository;

import com.goodspace.runny.domain.payment.entity.Payment;
import com.goodspace.runny.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 결제 리포지토리. 상태 전이는 조건부 UPDATE로 동시 승인 요청에도 1회만 처리되게 한다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByOrderIdAndUserId(String orderId, Long userId);

    /** 상태 조건부 전이 (from -> to). 영향 행 1이면 이번 요청이 처리 주체 (멱등/동시성 보장) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :to WHERE p.orderId = :orderId AND p.status = :from")
    int transition(@Param("orderId") String orderId,
                   @Param("from") PaymentStatus from,
                   @Param("to") PaymentStatus to);
}

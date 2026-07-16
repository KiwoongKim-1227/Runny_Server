package com.goodspace.runny.domain.coin.dto;

import com.goodspace.runny.domain.coin.entity.CoinTransaction;
import com.goodspace.runny.domain.coin.entity.CoinTransactionType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 코인 조회 응답 DTO 모음.
 */
public final class CoinDto {

    private CoinDto() {
    }

    /** 보유 코인 응답 */
    public record MeResponse(
            int coin
    ) {
    }

    /** 코인 내역 항목 - amount 양수는 적립, 음수는 차감 */
    public record TransactionItem(
            Long id,
            int amount,
            CoinTransactionType type,
            Long refId,
            LocalDateTime createdAt
    ) {
        public static TransactionItem from(CoinTransaction transaction) {
            return new TransactionItem(transaction.getId(), transaction.getAmount(),
                    transaction.getType(), transaction.getRefId(), transaction.getCreatedAt());
        }
    }

    /** 코인 내역 응답 - 전체 반환 (MVP 규모에서 페이징 제거) */
    public record TransactionsResponse(
            List<TransactionItem> content
    ) {
    }
}

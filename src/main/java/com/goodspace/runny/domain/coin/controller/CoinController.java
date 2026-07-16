package com.goodspace.runny.domain.coin.controller;

import com.goodspace.runny.domain.coin.dto.CoinDto;
import com.goodspace.runny.domain.coin.repository.CoinTransactionRepository;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 코인 API 컨트롤러. 보유 코인과 사용/획득 내역(원장) 조회를 제공한다.
 */
@Tag(name = "Coin", description = "재화 API - 보유 코인, 코인 원장 내역")
@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CoinController {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    /** 보유 코인 조회 */
    @Operation(summary = "보유 코인 조회")
    @GetMapping("/me")
    public ApiResponse<CoinDto.MeResponse> getMyCoin() {
        int coin = userRepository.findById(SecurityUtil.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003))
                .getCoin();
        return ApiResponse.ok(new CoinDto.MeResponse(coin));
    }

    /** 코인 사용/획득 내역 조회 (페이징) */
    @Operation(summary = "코인 내역 조회", description = "원장 기반 적립(+)/차감(-) 내역, 최신순 페이징")
    @GetMapping("/transactions")
    public ApiResponse<CoinDto.TransactionsResponse> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<com.goodspace.runny.domain.coin.entity.CoinTransaction> transactions =
                coinTransactionRepository.findByUserIdOrderByIdDesc(
                        SecurityUtil.currentUserId(), PageRequest.of(page, size));
        return ApiResponse.ok(new CoinDto.TransactionsResponse(
                transactions.getContent().stream().map(CoinDto.TransactionItem::from).toList(),
                transactions.getNumber(), transactions.getSize(),
                transactions.getTotalElements(), transactions.hasNext()));
    }
}

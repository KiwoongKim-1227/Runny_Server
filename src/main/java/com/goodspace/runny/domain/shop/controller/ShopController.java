package com.goodspace.runny.domain.shop.controller;

import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.payment.dto.PaymentDto;
import com.goodspace.runny.domain.payment.service.PaymentService;
import com.goodspace.runny.domain.shop.dto.ShopDto;
import com.goodspace.runny.domain.shop.service.ShopService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 상점 API 컨트롤러. 입장 조건 조회, 아이템 목록/구매, 코인 상품 조회를 제공한다.
 */
@Tag(name = "Shop", description = "상점 API - 꾸미기 아이템 구매(즉시 착용), 코인 상품")
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final PaymentService paymentService;

    /** 상점 입장 가능 여부 조회 */
    @Operation(summary = "상점 입장 가능 여부",
            description = "활성 강아지 레벨 10 이상 조건. 프론트 진입 차단용으로 예외 없이 accessible을 반환한다")
    @GetMapping("/access")
    public ApiResponse<ShopDto.AccessResponse> checkAccess() {
        return ApiResponse.ok(shopService.checkAccess(SecurityUtil.currentUserId()));
    }

    /** 카테고리별 전체 아이템 조회 */
    @Operation(summary = "카테고리별 전체 아이템 조회",
            description = "가격/등급(tier)/보유 여부(owned) 포함. 프론트는 tier로 등급 섹션 그룹핑, "
                    + "보유 아이템은 가격 대신 보유중 표시. 레벨 미달 시 SHOP_001")
    @GetMapping("/items")
    public ApiResponse<List<ShopDto.ShopItemResponse>> getItems(@RequestParam ItemCategory category) {
        return ApiResponse.ok(shopService.getItems(SecurityUtil.currentUserId(), category));
    }

    /** 아이템 단건/일괄 구매 */
    @Operation(summary = "아이템 단건/일괄 구매",
            description = "코인 차감 + 보유 등록 + 활성 강아지 즉시 착용까지 하나의 트랜잭션. "
                    + "이미 보유 아이템 포함 시 전체 실패 ITEM_004, 잔액 부족 COIN_001, 레벨 미달 SHOP_001")
    @PostMapping("/purchase")
    public ApiResponse<ShopDto.PurchaseResponse> purchase(@Valid @RequestBody ShopDto.PurchaseRequest request) {
        return ApiResponse.ok(shopService.purchase(SecurityUtil.currentUserId(), request));
    }

    /** 코인 상품 5종 조회 (코인 탭) */
    @Operation(summary = "코인 상품 조회", description = "NicePay 결제 대상 코인 상품 5종. 이후 결제는 /api/payments/orders로 진행")
    @GetMapping("/coin-products")
    public ApiResponse<List<PaymentDto.CoinProductResponse>> getCoinProducts() {
        return ApiResponse.ok(paymentService.getCoinProducts());
    }
}

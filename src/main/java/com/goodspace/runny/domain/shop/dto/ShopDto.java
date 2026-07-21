package com.goodspace.runny.domain.shop.dto;

import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.entity.ItemTier;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 상점 요청/응답 DTO 모음.
 */
public final class ShopDto {

    private ShopDto() {
    }

    /** 상점 입장 가능 여부 응답 - 프론트 진입 차단용 */
    public record AccessResponse(
            boolean accessible,
            int requiredLevel,
            int currentLevel
    ) {
    }

    /** 상점 아이템 응답 - 전체 아이템 + 가격 + 보유 여부. 프론트는 tier로 등급 섹션을 그룹핑한다 */
    public record ShopItemResponse(
            Long itemId,
            String name,
            ItemCategory category,
            ItemTier tier,
            int price,
            String modelUrl,
            boolean owned
    ) {
        public static ShopItemResponse of(Item item, boolean owned) {
            return new ShopItemResponse(item.getId(), item.getName(), item.getCategory(),
                    item.getTier(), item.getPrice(), item.getModelUrl(), owned);
        }
    }

    /** 구매 요청 - 아이템 ID 배열 (단건/일괄 공용) */
    public record PurchaseRequest(
            @NotEmpty List<Long> itemIds
    ) {
    }

    /** 구매 응답 - 구매한 아이템과 총 차감 코인 (구매 즉시 활성 강아지에 착용됨) */
    public record PurchaseResponse(
            List<Long> purchasedItemIds,
            int totalPrice
    ) {
    }
}

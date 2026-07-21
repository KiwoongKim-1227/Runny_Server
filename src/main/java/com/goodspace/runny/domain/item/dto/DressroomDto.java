package com.goodspace.runny.domain.item.dto;

import com.goodspace.runny.domain.dog.entity.DogEquipment;
import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.entity.ItemTier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 드레스룸 요청/응답 DTO 모음.
 */
public final class DressroomDto {

    private DressroomDto() {
    }

    /** 아이템 요약 - 드레스룸/상점에서 공통 재사용 */
    public record ItemSummary(
            Long itemId,
            String name,
            ItemCategory category,
            ItemTier tier,
            int price,
            String modelUrl
    ) {
        public static ItemSummary from(Item item) {
            return new ItemSummary(item.getId(), item.getName(), item.getCategory(),
                    item.getTier(), item.getPrice(), item.getModelUrl());
        }
    }

    /** 드레스룸 카테고리별 응답 - 보유 아이템 목록 + 현재 착용 아이템 ID(미착용 null) */
    public record CategoryItems(
            ItemCategory category,
            Long equippedItemId,
            List<ItemSummary> items
    ) {
    }

    /**
     * 코디 저장 요청 - 6개 슬롯의 itemId를 한 번에 저장. null은 해당 슬롯 해제.
     * 미포함 필드도 null로 역직렬화되므로 "요청 상태 = 최종 코디"인 전체 교체 방식이다.
     */
    public record EquipmentSaveRequest(
            Long collar,
            Long clothes,
            Long hat,
            Long shoes,
            Long toy,
            Long effect
    ) {
        /** 슬롯별 요청 값을 카테고리 맵으로 변환 (null 포함) */
        public Map<ItemCategory, Long> toSlotMap() {
            java.util.EnumMap<ItemCategory, Long> map = new java.util.EnumMap<>(ItemCategory.class);
            map.put(ItemCategory.COLLAR, collar);
            map.put(ItemCategory.CLOTHES, clothes);
            map.put(ItemCategory.HAT, hat);
            map.put(ItemCategory.SHOES, shoes);
            map.put(ItemCategory.TOY, toy);
            map.put(ItemCategory.EFFECT, effect);
            return map;
        }
    }

    /** 저장된 코디 응답 - 슬롯별 착용 아이템 ID (미착용 null). 되돌리기/초기화 용 */
    public record EquipmentResponse(
            Long collar,
            Long clothes,
            Long hat,
            Long shoes,
            Long toy,
            Long effect
    ) {
        public static EquipmentResponse from(List<DogEquipment> equipments) {
            return new EquipmentResponse(
                    findItemId(equipments, ItemCategory.COLLAR),
                    findItemId(equipments, ItemCategory.CLOTHES),
                    findItemId(equipments, ItemCategory.HAT),
                    findItemId(equipments, ItemCategory.SHOES),
                    findItemId(equipments, ItemCategory.TOY),
                    findItemId(equipments, ItemCategory.EFFECT)
            );
        }

        private static Long findItemId(List<DogEquipment> equipments, ItemCategory category) {
            return equipments.stream()
                    .filter(e -> e.getCategory() == category)
                    .findFirst()
                    .map(DogEquipment::getItemId)
                    .orElse(null);
        }
    }
}

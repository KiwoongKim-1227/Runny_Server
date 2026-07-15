package com.goodspace.runny.domain.item.service;

import com.goodspace.runny.domain.dog.entity.DogEquipment;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.repository.DogEquipmentRepository;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.domain.item.dto.DressroomDto;
import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.repository.ItemRepository;
import com.goodspace.runny.domain.item.repository.UserItemRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 드레스룸 서비스.
 * 핵심 규칙: 아이템 "보유"는 계정(user) 단위로 공유되지만, "착용 상태"는 강아지(user_dog) 단위로 저장된다.
 * 즉 어떤 강아지든 내 보유 아이템을 입힐 수 있고, 코디는 강아지마다 따로 유지된다.
 * 미리보기/즉시 착용/되돌리기는 프론트 로컬 상태로 처리하며, 되돌리기는 저장된 코디 재조회로 구현한다.
 */
@Service
@RequiredArgsConstructor
public class DressroomService {

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final DogEquipmentRepository dogEquipmentRepository;
    private final DogService dogService;

    /** 카테고리별 보유 아이템 목록 + 현재 착용 아이템 ID. 보유가 없으면 빈 배열 (프론트가 해제 아이콘만 노출) */
    @Transactional(readOnly = true)
    public DressroomDto.CategoryItems getOwnedItems(Long userId, ItemCategory category) {
        UserDog activeDog = dogService.getActiveDog(userId);
        List<DressroomDto.ItemSummary> items = userItemRepository
                .findByUserIdAndCategory(userId, category).stream()
                .map(userItem -> DressroomDto.ItemSummary.from(userItem.getItem()))
                .toList();
        Long equippedItemId = dogEquipmentRepository
                .findByUserDogIdAndCategory(activeDog.getId(), category)
                .map(DogEquipment::getItemId)
                .orElse(null);
        return new DressroomDto.CategoryItems(category, equippedItemId, items);
    }

    /**
     * 활성 강아지 코디 저장 - 6개 슬롯 전체 교체(PUT). itemId null은 해당 슬롯 해제.
     * 미보유 아이템(ITEM_002), 슬롯과 카테고리 불일치(ITEM_003)를 서버에서 검증한다.
     */
    @Transactional
    public DressroomDto.EquipmentResponse saveEquipment(Long userId, DressroomDto.EquipmentSaveRequest request) {
        UserDog activeDog = dogService.getActiveDog(userId);
        Map<ItemCategory, Long> slots = request.toSlotMap();

        // 착용 요청된 아이템 일괄 검증 (존재/카테고리 일치/보유)
        List<Long> requestedItemIds = slots.values().stream().filter(Objects::nonNull).toList();
        Map<Long, Item> itemsById = validateItems(userId, slots, requestedItemIds);

        // 기존 코디를 슬롯 맵으로 로드 후 슬롯별 upsert/delete
        Map<ItemCategory, DogEquipment> current = new EnumMap<>(ItemCategory.class);
        dogEquipmentRepository.findByUserDogId(activeDog.getId())
                .forEach(equipment -> current.put(equipment.getCategory(), equipment));

        slots.forEach((category, itemId) -> {
            DogEquipment equipped = current.get(category);
            if (itemId == null) {
                // 해제
                if (equipped != null) {
                    dogEquipmentRepository.delete(equipped);
                }
            } else if (equipped == null) {
                dogEquipmentRepository.save(new DogEquipment(activeDog.getId(), category, itemId));
            } else if (!itemId.equals(equipped.getItemId())) {
                equipped.changeItem(itemId);
            }
        });

        return getEquipment(userId);
    }

    /** 저장된 코디 조회 - 되돌리기/초기화는 이 API 재조회로 처리 */
    @Transactional(readOnly = true)
    public DressroomDto.EquipmentResponse getEquipment(Long userId) {
        UserDog activeDog = dogService.getActiveDog(userId);
        return DressroomDto.EquipmentResponse.from(dogEquipmentRepository.findByUserDogId(activeDog.getId()));
    }

    /** 요청 아이템 검증 - 존재(ITEM_001), 슬롯-카테고리 일치(ITEM_003), 계정 보유(ITEM_002) */
    private Map<Long, Item> validateItems(Long userId, Map<ItemCategory, Long> slots, List<Long> requestedItemIds) {
        if (requestedItemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Item> itemsById = new java.util.HashMap<>();
        itemRepository.findAllById(requestedItemIds).forEach(item -> itemsById.put(item.getId(), item));

        Set<Long> ownedIds = userItemRepository.findOwnedItemIds(userId, requestedItemIds);
        slots.forEach((category, itemId) -> {
            if (itemId == null) {
                return;
            }
            Item item = itemsById.get(itemId);
            if (item == null) {
                throw new BusinessException(ErrorCode.ITEM_001);
            }
            if (item.getCategory() != category) {
                throw new BusinessException(ErrorCode.ITEM_003);
            }
            if (!ownedIds.contains(itemId)) {
                throw new BusinessException(ErrorCode.ITEM_002);
            }
        });
        return itemsById;
    }
}

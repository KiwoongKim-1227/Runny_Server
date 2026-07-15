package com.goodspace.runny.domain.shop.service;

import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.dog.entity.DogEquipment;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.repository.DogEquipmentRepository;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.entity.UserItem;
import com.goodspace.runny.domain.item.repository.ItemRepository;
import com.goodspace.runny.domain.item.repository.UserItemRepository;
import com.goodspace.runny.domain.shop.dto.ShopDto;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 상점 서비스. 입장 조건(활성 강아지 레벨 10 이상)을 모든 상점 API에서 공통 검증하고,
 * 아이템 목록(보유 여부 포함)과 단건/일괄 구매(코인 차감 + 구매 즉시 착용)를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    // 상점 입장 최소 레벨
    private static final int REQUIRED_LEVEL = 10;

    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final DogEquipmentRepository dogEquipmentRepository;
    private final DogService dogService;
    private final CoinService coinService;

    /** 상점 입장 가능 여부 조회 - 예외 없이 조건/현재 레벨을 반환 (프론트 진입 차단용) */
    @Transactional(readOnly = true)
    public ShopDto.AccessResponse checkAccess(Long userId) {
        UserDog activeDog = dogService.getActiveDog(userId);
        return new ShopDto.AccessResponse(
                activeDog.getLevel() >= REQUIRED_LEVEL, REQUIRED_LEVEL, activeDog.getLevel());
    }

    /** 카테고리별 전체 아이템 조회 - 가격/등급/보유 여부 포함 */
    @Transactional(readOnly = true)
    public List<ShopDto.ShopItemResponse> getItems(Long userId, ItemCategory category) {
        UserDog activeDog = dogService.getActiveDog(userId);
        validateAccess(activeDog);

        List<Item> items = itemRepository.findByCategoryOrderByPriceAsc(category);
        Set<Long> ownedIds = userItemRepository.findOwnedItemIds(
                userId, items.stream().map(Item::getId).toList());
        return items.stream()
                .map(item -> ShopDto.ShopItemResponse.of(item, ownedIds.contains(item.getId())))
                .toList();
    }

    /**
     * 아이템 단건/일괄 구매 - 하나의 트랜잭션에서 미보유 검증, 총액 계산, 코인 조건부 차감,
     * user_item 등록, 활성 강아지 즉시 착용(슬롯 교체)까지 처리한다.
     * 이미 보유한 아이템이 섞여 있으면 전체 실패(ITEM_004) 정책.
     */
    @Transactional
    public ShopDto.PurchaseResponse purchase(Long userId, ShopDto.PurchaseRequest request) {
        UserDog activeDog = dogService.getActiveDog(userId);
        validateAccess(activeDog);

        // 중복 ID 제거 (동일 아이템 2개 요청은 1개 구매로 처리)
        List<Long> itemIds = List.copyOf(new LinkedHashSet<>(request.itemIds()));
        if (itemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOP_002);
        }

        // 존재 검증
        List<Item> items = itemRepository.findAllById(itemIds);
        if (items.size() != itemIds.size()) {
            throw new BusinessException(ErrorCode.ITEM_001);
        }
        // 이미 보유한 아이템 포함 시 전체 실패
        Set<Long> ownedIds = userItemRepository.findOwnedItemIds(userId, itemIds);
        if (!ownedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.ITEM_004);
        }

        // 총액 계산 후 코인 조건부 차감 (잔액 부족 시 COIN_001, 전체 롤백)
        int totalPrice = items.stream().mapToInt(Item::getPrice).sum();
        coinService.deduct(userId, totalPrice, CoinTransactionType.ITEM_PURCHASE, null);

        // 보유 등록 + 구매 즉시 착용 (같은 카테고리 복수 구매 시 마지막 아이템이 착용됨)
        items.forEach(item -> {
            userItemRepository.save(new UserItem(userId, item));
            equip(activeDog.getId(), item);
        });

        return new ShopDto.PurchaseResponse(items.stream().map(Item::getId).toList(), totalPrice);
    }

    /** 상점 공통 입장 조건 검증 - 미달 시 403성 비즈니스 에러(SHOP_001) */
    private void validateAccess(UserDog activeDog) {
        if (activeDog.getLevel() < REQUIRED_LEVEL) {
            throw new BusinessException(ErrorCode.SHOP_001);
        }
    }

    /** 활성 강아지 해당 카테고리 슬롯 착용/교체 (드레스룸과 동일한 슬롯 규칙) */
    private void equip(Long userDogId, Item item) {
        dogEquipmentRepository.findByUserDogIdAndCategory(userDogId, item.getCategory())
                .ifPresentOrElse(
                        equipment -> equipment.changeItem(item.getId()),
                        () -> dogEquipmentRepository.save(
                                new DogEquipment(userDogId, item.getCategory(), item.getId())));
    }
}

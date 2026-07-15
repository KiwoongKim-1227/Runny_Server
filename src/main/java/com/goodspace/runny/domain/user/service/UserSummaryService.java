package com.goodspace.runny.domain.user.service;

import com.goodspace.runny.domain.dog.entity.DogEquipment;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.repository.DogEquipmentRepository;
import com.goodspace.runny.domain.dog.repository.UserDogRepository;
import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.repository.ItemRepository;
import com.goodspace.runny.domain.user.dto.UserSummary;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * UserSummary 조립 공용 서비스. 유저/활성 강아지/착용 코디/아이템 이미지를 일괄 조회해
 * N+1 없이 요약 목록을 만든다. 친구/놀이터와 크루(7단계) 응답에서 공통 사용한다.
 */
@Service
@RequiredArgsConstructor
public class UserSummaryService {

    private final UserRepository userRepository;
    private final UserDogRepository userDogRepository;
    private final DogEquipmentRepository dogEquipmentRepository;
    private final ItemRepository itemRepository;

    /** 단건 요약 조회 */
    @Transactional(readOnly = true)
    public UserSummary summarize(Long userId) {
        Map<Long, UserSummary> result = summarizeAll(List.of(userId));
        return result.get(userId);
    }

    /** 여러 유저의 요약 일괄 조립 - 입력 순서를 유지한 Map 반환 (탈퇴 유저는 제외됨) */
    @Transactional(readOnly = true)
    public Map<Long, UserSummary> summarizeAll(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userRepository.findAllById(userIds);

        // 활성 강아지 일괄 조회
        List<Long> activeDogIds = users.stream()
                .map(User::getActiveDogId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, UserDog> dogsById = new HashMap<>();
        userDogRepository.findAllById(activeDogIds).forEach(dog -> dogsById.put(dog.getId(), dog));

        // 착용 코디 + 아이템 이미지 일괄 조회
        Map<Long, List<DogEquipment>> equipmentsByDogId = new HashMap<>();
        List<DogEquipment> equipments = activeDogIds.isEmpty()
                ? List.of()
                : dogEquipmentRepository.findByUserDogIdIn(activeDogIds);
        equipments.forEach(equipment ->
                equipmentsByDogId.computeIfAbsent(equipment.getUserDogId(), k -> new java.util.ArrayList<>())
                        .add(equipment));
        Map<Long, Item> itemsById = new HashMap<>();
        List<Long> itemIds = equipments.stream().map(DogEquipment::getItemId).distinct().toList();
        if (!itemIds.isEmpty()) {
            itemRepository.findAllById(itemIds).forEach(item -> itemsById.put(item.getId(), item));
        }

        Map<Long, UserSummary> result = new LinkedHashMap<>();
        for (Long userId : userIds) {
            users.stream().filter(u -> u.getId().equals(userId)).findFirst().ifPresent(user -> {
                UserDog dog = user.getActiveDogId() == null ? null : dogsById.get(user.getActiveDogId());
                result.put(userId, new UserSummary(user.getId(), user.getNickname(), toDogSummary(
                        dog, equipmentsByDogId.getOrDefault(dog == null ? null : dog.getId(), List.of()), itemsById)));
            });
        }
        return result;
    }

    /** 강아지 요약 변환 - 견종 이미지 + 착용 아이템 이미지 목록 */
    private UserSummary.DogSummary toDogSummary(UserDog dog, List<DogEquipment> equipments,
                                                Map<Long, Item> itemsById) {
        if (dog == null) {
            return null;
        }
        List<UserSummary.EquippedItem> equippedItems = equipments.stream()
                .map(equipment -> {
                    Item item = itemsById.get(equipment.getItemId());
                    return new UserSummary.EquippedItem(equipment.getCategory(), equipment.getItemId(),
                            item == null ? null : item.getImageUrl());
                })
                .toList();
        return new UserSummary.DogSummary(dog.getId(), dog.getName(), dog.getBreed().getName(),
                dog.getLevel(), dog.getBreed().getImageUrl(), equippedItems);
    }
}

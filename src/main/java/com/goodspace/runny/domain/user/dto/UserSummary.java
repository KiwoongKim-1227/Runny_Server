package com.goodspace.runny.domain.user.dto;

import com.goodspace.runny.domain.item.entity.ItemCategory;

import java.util.List;

/**
 * 유저 요약 공통 DTO. 닉네임 + 활성 강아지(이름/견종명/레벨/외형 이미지 정보)를 담으며
 * 친구 목록/검색/상세와 크루 상세/TOP3/멤버 목록/가입 요청 목록 응답에서 공통 재사용한다.
 */
public record UserSummary(
        Long userId,
        String nickname,
        DogSummary dog
) {

    /** 활성 강아지 요약 - 외형은 견종 이미지 + 착용 아이템 이미지 목록으로 합성한다(프론트 렌더링) */
    public record DogSummary(
            Long dogId,
            String name,
            String breedName,
            int level,
            String breedImageUrl,
            List<EquippedItem> equippedItems
    ) {
    }

    /** 착용 아이템 외형 정보 */
    public record EquippedItem(
            ItemCategory category,
            Long itemId,
            String imageUrl
    ) {
    }
}

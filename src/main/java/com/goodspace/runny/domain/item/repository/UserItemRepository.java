package com.goodspace.runny.domain.item.repository;

import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * 보유 아이템 리포지토리. 보유는 계정 단위 공유.
 */
public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    /** 카테고리별 보유 아이템 조회 (드레스룸 목록용) */
    @Query("SELECT ui FROM UserItem ui JOIN FETCH ui.item i " +
            "WHERE ui.userId = :userId AND i.category = :category ORDER BY i.price ASC")
    List<UserItem> findByUserIdAndCategory(@Param("userId") Long userId,
                                           @Param("category") ItemCategory category);

    /** 유저가 보유한 아이템 ID 집합 조회 (착용 저장 검증/상점 보유 표시용) */
    @Query("SELECT ui.item.id FROM UserItem ui WHERE ui.userId = :userId AND ui.item.id IN :itemIds")
    Set<Long> findOwnedItemIds(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);

    boolean existsByUserIdAndItemId(Long userId, Long itemId);
}

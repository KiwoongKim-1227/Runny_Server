package com.goodspace.runny.domain.item.repository;

import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 아이템 마스터 리포지토리.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByCategoryOrderByPriceAsc(ItemCategory category);
}

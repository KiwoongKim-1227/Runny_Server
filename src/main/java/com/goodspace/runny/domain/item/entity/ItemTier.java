package com.goodspace.runny.domain.item.entity;

/**
 * 아이템 등급. 가격은 등급 단일가가 아닌 등급 범위 내 분포형으로 시드에서 개별 부여한다.
 * 저렴(CHEAP) 60~199 / 중간(MID) 200~999 / 고급(PREMIUM) 1000~2000.
 */
public enum ItemTier {
    CHEAP, MID, PREMIUM
}

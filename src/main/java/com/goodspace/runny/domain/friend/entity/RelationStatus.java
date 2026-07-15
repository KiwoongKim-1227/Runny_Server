package com.goodspace.runny.domain.friend.entity;

/**
 * 검색 결과에서 나와 상대의 관계 상태. 프론트 버튼 분기용
 * (NONE: + 버튼 / REQUESTED: 내가 요청함 / RECEIVED: 상대가 나에게 요청함 / FRIEND: 이미 친구).
 */
public enum RelationStatus {
    NONE, REQUESTED, RECEIVED, FRIEND
}

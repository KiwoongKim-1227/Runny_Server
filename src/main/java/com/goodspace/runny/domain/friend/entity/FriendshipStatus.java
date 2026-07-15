package com.goodspace.runny.domain.friend.entity;

/**
 * 친구 관계 상태. 거절은 행 삭제로 처리해 재요청이 가능하다.
 */
public enum FriendshipStatus {
    PENDING, ACCEPTED
}

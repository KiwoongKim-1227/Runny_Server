package com.goodspace.runny.domain.notification.entity;

/**
 * 알림 타입. MVP는 2종이지만 확장형 구조(type + payload JSON)라
 * 추후 퀘스트 완료/업적/결제/공지 등이 추가돼도 컬럼 변경 없이 타입만 추가하면 된다.
 */
public enum NotificationType {
    FRIEND_REQUEST, CREW_APPROVED
}

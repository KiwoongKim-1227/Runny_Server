package com.goodspace.runny.domain.crew.entity;

/**
 * 가입 신청 상태. 목록 조회는 PENDING만 반환하며 처리된 요청은 즉시 목록에서 제외된다.
 */
public enum JoinRequestStatus {
    PENDING, APPROVED, REJECTED
}

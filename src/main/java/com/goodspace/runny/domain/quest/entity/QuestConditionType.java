package com.goodspace.runny.domain.quest.entity;

/**
 * 퀘스트 달성 조건 유형. 진행도 갱신 트리거와 매칭된다.
 * LOGIN: 접속(조회 시 자동 달성) / DISTANCE: 1회 러닝 거리 / DURATION: 1회 러닝 시간(분)
 * NONSTOP: 1회 최장 무정지 시간(분) / RUN_COUNT: 러닝 횟수 / WEEK_DISTANCE: 주간 누적 거리 / DECORATE: 히스토리 꾸미기
 */
public enum QuestConditionType {
    LOGIN, DISTANCE, DURATION, NONSTOP, RUN_COUNT, WEEK_DISTANCE, DECORATE
}

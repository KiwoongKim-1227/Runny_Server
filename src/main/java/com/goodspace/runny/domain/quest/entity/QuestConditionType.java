package com.goodspace.runny.domain.quest.entity;

/**
 * 퀘스트 달성 조건 유형. 진행도 갱신 트리거와 매칭된다.
 * 일일: LOGIN(접속, 조회 시 자동 달성) / DISTANCE(1회 거리) / DURATION(1회 시간, 분) / NONSTOP(1회 무정지, 분 - 주간 공용)
 *      / RUN_COUNT(러닝 완료 횟수) / DECORATE(사진 꾸미기 완료) / DECORATE_ENTER(꾸미기 창 접속)
 *      / NEW_ROUTE(새 경로 1km 이상 - 신규 경로 여부는 프론트 판정) / STEADY_PACE(일정 페이스 유지 거리 km - 프론트 계산)
 * 주간: WEEK_RUN_1KM(1km 이상 러닝 횟수) / WEEK_LOGIN_DAYS(접속 일수) / WEEKEND_DISTANCE(주말 누적 거리)
 *      / WEEK_DISTANCE(주간 누적 거리) / WEEK_DURATION(주간 누적 시간, 분) / WEEK_CALORIES(주간 누적 칼로리)
 *      / LAST_WEEK_BEAT(지난주 총 거리 초과 - target은 출제 시 지난주 거리 스냅샷)
 */
public enum QuestConditionType {
    LOGIN, DISTANCE, DURATION, NONSTOP, RUN_COUNT, DECORATE, DECORATE_ENTER, NEW_ROUTE, STEADY_PACE,
    WEEK_RUN_1KM, WEEK_LOGIN_DAYS, WEEKEND_DISTANCE, WEEK_DISTANCE, WEEK_DURATION, WEEK_CALORIES, LAST_WEEK_BEAT
}

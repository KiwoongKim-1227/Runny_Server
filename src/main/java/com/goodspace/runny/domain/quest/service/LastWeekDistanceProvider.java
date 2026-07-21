package com.goodspace.runny.domain.quest.service;

/**
 * 지난주(월~일, KST) 총 러닝 거리 제공자 인터페이스.
 * "지난주 총 러닝 거리보다 많이 달리기" 주간 퀘스트의 출제 시점 target 스냅샷에 사용한다.
 * 실제 구현은 러닝 도메인(LastWeekDistanceProviderImpl)이 담당해 도메인 순환 참조를 피한다.
 */
public interface LastWeekDistanceProvider {

    /** 지난주 월요일 00:00 ~ 이번 주 월요일 00:00 (KST) 러닝 거리 합계 (기록 없으면 0) */
    double lastWeekDistanceKm(Long userId);
}

package com.goodspace.runny.domain.quest.entity;

/**
 * 퀘스트 구분. 일일 고정(매일 00:00 초기화) / 일일 랜덤(매일 2개 랜덤 출제) / 주간(매주 월요일 초기화).
 */
public enum QuestType {
    DAILY_FIXED, DAILY_RANDOM, WEEKLY
}

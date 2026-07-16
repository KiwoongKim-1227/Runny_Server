package com.goodspace.runny.domain.quest.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;

/**
 * 퀘스트 기간 키 유틸. 일일은 yyyy-MM-dd, 주간은 yyyy-Www(ISO 주차, 월요일 시작) 형식이며 KST 기준이다.
 */
public final class QuestPeriod {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private QuestPeriod() {
    }

    /** 오늘 키 (예: 2026-07-16) */
    public static String todayKey() {
        return LocalDate.now(ZONE_SEOUL).toString();
    }

    /** 이번 주 키 (예: 2026-W29) - ISO 주 기준(월요일 00:00 시작) */
    public static String weekKey() {
        LocalDate now = LocalDate.now(ZONE_SEOUL);
        return "%d-W%02d".formatted(
                now.get(IsoFields.WEEK_BASED_YEAR),
                now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}

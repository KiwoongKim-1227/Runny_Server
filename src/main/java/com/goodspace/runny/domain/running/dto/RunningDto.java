package com.goodspace.runny.domain.running.dto;

import com.goodspace.runny.domain.achievement.dto.AchievementDto;
import com.goodspace.runny.domain.running.entity.RunningRecord;
import com.goodspace.runny.domain.running.entity.Sticker;
import com.goodspace.runny.domain.user.dto.UserSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 러닝 요청/응답 DTO 모음. 리포트는 꾸미기 위젯용으로 전 지표를 개별 필드로 반환한다.
 */
public final class RunningDto {

    private RunningDto() {
    }

    /** 세션 시작 응답 */
    public record StartResponse(
            Long sessionId,
            LocalDateTime startedAt
    ) {
    }

    /** 러닝 종료 요청 - 프론트가 측정한 최종 집계 데이터 */
    public record CompleteRequest(
            @NotBlank String clientRunId,
            @NotNull Double distanceKm,
            @NotNull Long durationSec,
            @NotNull Long avgPaceSec,
            Integer cadence,
            Integer avgHeartRate,
            @NotNull Long longestNonstopSec,
            List<Integer> splitPaces,
            Double elevationM,
            String routeImageUrl,
            String routeLineImageUrl,
            @NotNull LocalDateTime startedAt,
            @NotNull LocalDateTime endedAt,
            List<Long> visitedLandmarkIds,
            // 새로운 경로 러닝 여부 - 신규 경로 판정은 프론트 책임 (일일 랜덤 퀘스트 "새로운 경로로 1km 이상 달리기")
            Boolean newRoute,
            // 일정한 페이스를 유지한 최장 거리(km) - 프론트 계산 전달 (일일 랜덤 퀘스트 "일정한 페이스로 2km 유지하기")
            Double steadyPaceKm
    ) {
    }

    /** 스탯 변화량 */
    public record StatDelta(
            int stamina,
            int endurance,
            int speed
    ) {
    }

    /** 러닝 종료 응답 - 리포트 + 스탯 변화 + 이번 러닝으로 달성된 퀘스트/업적 요약 */
    public record CompleteResponse(
            boolean discarded,
            boolean idempotent,
            ReportResponse report,
            StatDelta statDelta,
            List<String> completedQuests,
            List<AchievementDto.AchievedItem> achievedAchievements
    ) {
        /** 최소 거리(0.03km) 미달 폐기 응답 - 리포트 미생성 */
        public static CompleteResponse ofDiscarded() {
            return new CompleteResponse(true, false, null, null, List.of(), List.of());
        }
    }

    /** 리포트 상세 - 전 지표 개별 필드(꾸미기 위젯용), 심박수 null 그대로, 강아지는 스냅샷 기반 */
    public record ReportResponse(
            Long recordId,
            String title,
            double distanceKm,
            long durationSec,
            long avgPaceSec,
            int calories,
            int cadence,
            Integer avgHeartRate,
            long longestNonstopSec,
            double elevationM,
            List<SplitItem> splits,
            String routeImageUrl,
            String routeLineImageUrl,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            StatDelta statDelta,
            String dogName,
            UserSummary.DogSummary dogAppearance
    ) {
        public static ReportResponse of(RunningRecord record, List<SplitItem> splits,
                                        UserSummary.DogSummary dogAppearance) {
            return new ReportResponse(
                    record.getId(), record.getTitle(),
                    record.getDistanceKm(), record.getDurationSec(), record.getAvgPaceSec(),
                    record.getCalories(), record.getCadence(), record.getAvgHeartRate(),
                    record.getLongestNonstopSec(), record.getElevationM(),
                    splits, record.getRouteImageUrl(), record.getRouteLineImageUrl(),
                    record.getStartedAt(), record.getEndedAt(),
                    new StatDelta(record.getStaminaDelta(), record.getEnduranceDelta(), record.getSpeedDelta()),
                    record.getDogNameSnapshot(), dogAppearance);
        }
    }

    /** 구간별 페이스 항목 */
    public record SplitItem(
            int kmIndex,
            int paceSec
    ) {
    }

    /** 월별 히스토리 응답 - 요약 + 기록 목록 */
    public record HistoryResponse(
            int year,
            int month,
            Summary summary,
            List<HistoryItem> records
    ) {
        public record Summary(double totalDistanceKm, int runCount, long totalDurationSec) {
        }
    }

    /** 히스토리 기록 항목 - 미확인(isChecked=false) 여부 포함 */
    public record HistoryItem(
            Long recordId,
            String title,
            double distanceKm,
            long durationSec,
            long avgPaceSec,
            String routeImageUrl,
            LocalDateTime endedAt,
            boolean checked
    ) {
        public static HistoryItem from(RunningRecord record) {
            return new HistoryItem(record.getId(), record.getTitle(), record.getDistanceKm(),
                    record.getDurationSec(), record.getAvgPaceSec(), record.getRouteImageUrl(),
                    record.getEndedAt(), record.isChecked());
        }
    }

    /** 미확인 리포트 존재 여부 (홈 빨간 점) */
    public record UncheckedExists(
            boolean exists
    ) {
    }

    /** 스티커 항목 */
    public record StickerItem(
            Long stickerId,
            String name,
            String imageUrl
    ) {
        public static StickerItem from(Sticker sticker) {
            return new StickerItem(sticker.getId(), sticker.getName(), sticker.getImageUrl());
        }
    }
}

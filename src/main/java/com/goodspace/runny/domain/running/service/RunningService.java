package com.goodspace.runny.domain.running.service;

import com.goodspace.runny.domain.achievement.dto.AchievementDto;
import com.goodspace.runny.domain.achievement.service.AchievementService;
import com.goodspace.runny.domain.crew.repository.CrewRepository;
import com.goodspace.runny.domain.crew.repository.CrewMemberRepository;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.service.DogChangeLogService;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.domain.dog.util.StatCalculator;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
import com.goodspace.runny.domain.quest.service.QuestPeriod;
import com.goodspace.runny.domain.quest.service.QuestProgressService;
import com.goodspace.runny.domain.running.dto.RunningDto;
import com.goodspace.runny.domain.running.entity.RunningRecord;
import com.goodspace.runny.domain.running.entity.RunningSession;
import com.goodspace.runny.domain.running.entity.RunningSplit;
import com.goodspace.runny.domain.running.repository.RunningRecordRepository;
import com.goodspace.runny.domain.running.repository.RunningSessionRepository;
import com.goodspace.runny.domain.running.repository.RunningSplitRepository;
import com.goodspace.runny.domain.running.repository.StickerRepository;
import com.goodspace.runny.domain.user.dto.UserSummary;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.domain.user.service.UserSummaryService;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 러닝 서비스. 세션 시작, 종료 데이터 수신(리포트 생성 + 스탯/퀘스트/업적/크루 후속 처리 - 하나의 트랜잭션),
 * 리포트 상세(읽음 처리), 월별 히스토리, 미확인 여부, 스티커 목록을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunningService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    // 최소 저장 기준 거리 (미만이면 세션 폐기)
    private static final double MIN_DISTANCE_KM = 0.03;
    // 비정상 페이스 검증 범위 (초/km)
    private static final long MIN_PACE_SEC = 60;
    private static final long MAX_PACE_SEC = 3_600;
    // 미래 시각 허용 오차 (기기 시계 오차 보정)
    private static final long FUTURE_SKEW_SEC = 60;
    // 칼로리 공식 계수 (프론트 실시간 표시와 동일 공식, 최종값은 서버 계산)
    private static final double CALORIE_FACTOR = 1.036;
    private static final int DEFAULT_WEIGHT_KG = 60;

    private final RunningRecordRepository runningRecordRepository;
    private final RunningSplitRepository runningSplitRepository;
    private final RunningSessionRepository runningSessionRepository;
    private final StickerRepository stickerRepository;
    private final UserRepository userRepository;
    private final UserQuestRepository userQuestRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewRepository crewRepository;
    private final DogService dogService;
    private final DogChangeLogService dogChangeLogService;
    private final UserSummaryService userSummaryService;
    private final QuestProgressService questProgressService;
    private final AchievementService achievementService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** 러닝 세션 시작 (선택적) - 시작 시각과 사용 강아지를 기록. 실패 시에도 종료 API만으로 기록 생성 가능 */
    @Transactional
    public RunningDto.StartResponse start(Long userId) {
        UserDog activeDog = dogService.getActiveDog(userId);
        RunningSession session = runningSessionRepository.save(new RunningSession(userId, activeDog.getId()));
        return new RunningDto.StartResponse(session.getId(), session.getStartedAt());
    }

    /**
     * 러닝 종료 처리 (하나의 트랜잭션).
     * (1) 멱등키 중복 -> 기존 기록 반환 (2) 최소 거리 미달 -> 폐기 (3) 데이터 검증 (4) 칼로리 계산
     * (5) 제목 생성 (6) 스탯 반영 + delta 저장 + RUNNING 변화 로그 (7) 퀘스트 진행도 (8) 업적 판정
     * (9) 크루 누적 거리 (10) 강아지 외형 스냅샷 저장.
     */
    @Transactional
    public RunningDto.CompleteResponse complete(Long userId, RunningDto.CompleteRequest request) {
        // (1) 멱등 처리: 동일 clientRunId는 기존 기록 반환 (오프라인 재동기화 중복 방지)
        RunningRecord existing = runningRecordRepository.findByClientRunId(request.clientRunId()).orElse(null);
        if (existing != null) {
            return new RunningDto.CompleteResponse(false, true, buildReport(existing),
                    new RunningDto.StatDelta(existing.getStaminaDelta(), existing.getEnduranceDelta(),
                            existing.getSpeedDelta()),
                    List.of(), List.of());
        }

        // (2) 최소 거리 미달 -> 저장하지 않고 폐기 응답 (리포트 미생성)
        if (request.distanceKm() < MIN_DISTANCE_KM) {
            return RunningDto.CompleteResponse.discarded();
        }

        // (3) 데이터 검증 (음수/미래 시각/비정상 페이스)
        validate(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
        UserDog activeDog = dogService.getActiveDog(userId);

        // (4) 칼로리 = 1.036 x 체중(kg) x 거리(km), 서버 계산 최종값
        int weight = user.getWeight() != null ? user.getWeight() : DEFAULT_WEIGHT_KG;
        int calories = (int) Math.round(CALORIE_FACTOR * weight * request.distanceKm());

        // (5) 제목 생성 - "{강아지이름}와 함께한 {요일} {시간대} 러닝" (종료 시각 KST 기준)
        String title = buildTitle(activeDog.getName(), request.endedAt());

        // (6) 스탯 증가 계산(3단계 StatCalculator) -> 강아지 반영 + RUNNING 변화 로그 기록
        StatCalculator.StatDelta delta = StatCalculator.calculate(
                request.distanceKm(), request.durationSec(), request.avgPaceSec());
        activeDog.addStats(delta.stamina(), delta.endurance(), delta.speed());
        dogChangeLogService.recordStatChange(activeDog.getId(),
                delta.stamina(), delta.endurance(), delta.speed(), activeDog.getLevel());

        // (10) 강아지 외형 스냅샷 - 재조회 시 당시 모습 유지 (UserSummary의 DogSummary를 JSON 직렬화)
        UserSummary summary = userSummaryService.summarize(userId);
        String appearanceSnapshot = serializeAppearance(summary);

        RunningRecord record = runningRecordRepository.save(RunningRecord.builder()
                .userId(userId).userDogId(activeDog.getId()).clientRunId(request.clientRunId())
                .distanceKm(request.distanceKm()).durationSec(request.durationSec())
                .avgPaceSec(request.avgPaceSec()).calories(calories)
                .cadence(request.cadence() != null ? request.cadence() : 0)
                .avgHeartRate(request.avgHeartRate())
                .longestNonstopSec(request.longestNonstopSec())
                .elevationM(request.elevationM() != null ? request.elevationM() : 0)
                .routeImageUrl(request.routeImageUrl()).routeLineImageUrl(request.routeLineImageUrl())
                .title(title).startedAt(request.startedAt()).endedAt(request.endedAt())
                .staminaDelta(delta.stamina()).enduranceDelta(delta.endurance()).speedDelta(delta.speed())
                .dogNameSnapshot(activeDog.getName()).dogAppearanceSnapshot(appearanceSnapshot)
                .build());
        saveSplits(record.getId(), request.splitPaces());

        // (7) 퀘스트 진행도 갱신 - 이번 러닝으로 새로 달성된 퀘스트 요약을 위해 전후 비교
        Set<Long> completedBefore = completedQuestIds(userId);
        questProgressService.onRunningCompleted(userId, new QuestProgressService.RunningEvent(
                request.distanceKm(), request.durationSec(), request.longestNonstopSec()));
        List<String> newlyCompletedQuests = newlyCompletedTitles(userId, completedBefore);

        // (8) 업적 판정 - 어제 평균 페이스 비교 포함, 새로 달성된 업적 목록 반환
        Long yesterdayAvgPace = yesterdayAvgPace(userId);
        List<AchievementDto.AchievedItem> achieved = achievementService.evaluateRunning(userId,
                new AchievementService.RunningResult(
                        request.distanceKm(), request.durationSec(), request.longestNonstopSec(),
                        request.avgPaceSec(), yesterdayAvgPace, request.visitedLandmarkIds()));

        // (9) 크루 소속이면 크루 총 누적 거리 가산
        crewMemberRepository.findByUserId(userId).ifPresent(member ->
                crewRepository.findById(member.getCrewId())
                        .ifPresent(crew -> crew.addDistance(request.distanceKm())));

        return new RunningDto.CompleteResponse(false, false, buildReport(record),
                new RunningDto.StatDelta(delta.stamina(), delta.endurance(), delta.speed()),
                newlyCompletedQuests, achieved);
    }

    /** 리포트 상세 조회 - 전 지표 개별 필드 + 스냅샷 기반 강아지 정보, 조회 시 읽음(is_checked) 처리 */
    @Transactional
    public RunningDto.ReportResponse getReport(Long userId, Long recordId) {
        RunningRecord record = runningRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RUNNING_003));
        record.markChecked();
        return buildReport(record);
    }

    /** 월별 히스토리 - 요약(누적 거리/횟수/총 시간) + 해당 월 기록 목록(미확인 여부 포함) */
    @Transactional(readOnly = true)
    public RunningDto.HistoryResponse getHistory(Long userId, int year, int month) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);
        List<RunningRecord> records = runningRecordRepository
                .findByUserIdAndEndedAtBetweenOrderByEndedAtDesc(userId, start, end);

        double totalDistance = records.stream().mapToDouble(RunningRecord::getDistanceKm).sum();
        long totalDuration = records.stream().mapToLong(RunningRecord::getDurationSec).sum();
        return new RunningDto.HistoryResponse(year, month,
                new RunningDto.HistoryResponse.Summary(
                        Math.round(totalDistance * 100) / 100.0, records.size(), totalDuration),
                records.stream().map(RunningDto.HistoryItem::from).toList());
    }

    /** 미확인 리포트 존재 여부 (홈 화면 빨간 점) */
    @Transactional(readOnly = true)
    public boolean uncheckedExists(Long userId) {
        return runningRecordRepository.existsByUserIdAndCheckedFalse(userId);
    }

    /** 꾸미기 스티커 목록 */
    @Transactional(readOnly = true)
    public List<RunningDto.StickerItem> getStickers() {
        return stickerRepository.findAll().stream().map(RunningDto.StickerItem::from).toList();
    }

    /** 데이터 검증 - 음수/미래 시각/비정상 페이스/시각 순서 */
    private void validate(RunningDto.CompleteRequest request) {
        if (request.distanceKm() <= 0 || request.durationSec() <= 0
                || request.longestNonstopSec() < 0
                || request.longestNonstopSec() > request.durationSec()
                || (request.cadence() != null && request.cadence() < 0)
                || (request.avgHeartRate() != null && request.avgHeartRate() <= 0)) {
            throw new BusinessException(ErrorCode.RUNNING_001);
        }
        if (request.avgPaceSec() < MIN_PACE_SEC || request.avgPaceSec() > MAX_PACE_SEC) {
            throw new BusinessException(ErrorCode.RUNNING_001);
        }
        if (request.splitPaces() != null && request.splitPaces().stream().anyMatch(pace -> pace == null || pace <= 0)) {
            throw new BusinessException(ErrorCode.RUNNING_001);
        }
        LocalDateTime now = LocalDateTime.now(ZONE_SEOUL).plusSeconds(FUTURE_SKEW_SEC);
        if (request.endedAt().isAfter(now) || request.startedAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.RUNNING_002);
        }
        if (!request.startedAt().isBefore(request.endedAt())) {
            throw new BusinessException(ErrorCode.RUNNING_001);
        }
    }

    /** 제목 생성 - 시간대: 새벽 00~06 / 아침 06~12 / 오후 12~18 / 저녁 18~24 (KST, 종료 시각 기준) */
    private String buildTitle(String dogName, LocalDateTime endedAt) {
        String dayOfWeek = koreanDay(endedAt.getDayOfWeek());
        int hour = endedAt.getHour();
        String slot;
        if (hour < 6) {
            slot = "새벽";
        } else if (hour < 12) {
            slot = "아침";
        } else if (hour < 18) {
            slot = "오후";
        } else {
            slot = "저녁";
        }
        return "%s와 함께한 %s %s 러닝".formatted(dogName, dayOfWeek, slot);
    }

    private String koreanDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    /** 구간별 페이스 저장 (km_index 1부터) */
    private void saveSplits(Long recordId, List<Integer> splitPaces) {
        if (splitPaces == null || splitPaces.isEmpty()) {
            return;
        }
        List<RunningSplit> splits = new ArrayList<>();
        for (int i = 0; i < splitPaces.size(); i++) {
            splits.add(new RunningSplit(recordId, i + 1, splitPaces.get(i)));
        }
        runningSplitRepository.saveAll(splits);
    }

    /** 리포트 응답 조립 - 구간 페이스 + 스냅샷 역직렬화 */
    private RunningDto.ReportResponse buildReport(RunningRecord record) {
        List<RunningDto.SplitItem> splits = runningSplitRepository
                .findByRunningRecordIdOrderByKmIndexAsc(record.getId()).stream()
                .map(split -> new RunningDto.SplitItem(split.getKmIndex(), split.getPaceSec()))
                .toList();
        return RunningDto.ReportResponse.of(record, splits, deserializeAppearance(record));
    }

    /** 강아지 외형 스냅샷 직렬화 (Jackson 3 JsonMapper) */
    private String serializeAppearance(UserSummary summary) {
        try {
            return summary != null && summary.dog() != null
                    ? jsonMapper.writeValueAsString(summary.dog()) : null;
        } catch (Exception e) {
            log.warn("강아지 외형 스냅샷 직렬화 실패", e);
            return null;
        }
    }

    /** 강아지 외형 스냅샷 역직렬화 - 실패 시 null (프론트는 이름 스냅샷만 표시) */
    private UserSummary.DogSummary deserializeAppearance(RunningRecord record) {
        if (record.getDogAppearanceSnapshot() == null) {
            return null;
        }
        try {
            return jsonMapper.readValue(record.getDogAppearanceSnapshot(), UserSummary.DogSummary.class);
        } catch (Exception e) {
            log.warn("강아지 외형 스냅샷 역직렬화 실패: recordId={}", record.getId(), e);
            return null;
        }
    }

    /** 현재 기간(오늘+이번 주)의 달성 퀘스트 ID 집합 - 신규 달성 비교용 */
    private Set<Long> completedQuestIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        List.of(QuestPeriod.todayKey(), QuestPeriod.weekKey()).forEach(key ->
                userQuestRepository.findByUserIdAndPeriodKey(userId, key).stream()
                        .filter(UserQuest::isCompleted)
                        .map(UserQuest::getId)
                        .forEach(ids::add));
        return ids;
    }

    /** 이번 러닝으로 새로 달성된 퀘스트 제목 목록 */
    private List<String> newlyCompletedTitles(Long userId, Set<Long> completedBefore) {
        return List.of(QuestPeriod.todayKey(), QuestPeriod.weekKey()).stream()
                .flatMap(key -> userQuestRepository.findByUserIdAndPeriodKey(userId, key).stream())
                .filter(UserQuest::isCompleted)
                .filter(uq -> !completedBefore.contains(uq.getId()))
                .map(uq -> uq.getQuest().getTitle())
                .collect(Collectors.toList());
    }

    /** 어제(KST) 러닝들의 평균 페이스 - 기록 없으면 null */
    private Long yesterdayAvgPace(Long userId) {
        LocalDate yesterday = LocalDate.now(ZONE_SEOUL).minusDays(1);
        Double avg = runningRecordRepository.avgPaceBetween(userId,
                yesterday.atStartOfDay(), yesterday.plusDays(1).atStartOfDay());
        return avg == null ? null : Math.round(avg);
    }
}

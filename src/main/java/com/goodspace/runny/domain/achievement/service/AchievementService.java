package com.goodspace.runny.domain.achievement.service;

import com.goodspace.runny.domain.achievement.dto.AchievementDto;
import com.goodspace.runny.domain.achievement.entity.Achievement;
import com.goodspace.runny.domain.achievement.entity.AchievementProgress;
import com.goodspace.runny.domain.achievement.entity.RewardType;
import com.goodspace.runny.domain.achievement.entity.UserAchievement;
import com.goodspace.runny.domain.achievement.entity.UserLandmarkVisit;
import com.goodspace.runny.domain.achievement.repository.AchievementProgressRepository;
import com.goodspace.runny.domain.achievement.repository.AchievementRepository;
import com.goodspace.runny.domain.achievement.repository.UserAchievementRepository;
import com.goodspace.runny.domain.achievement.repository.UserLandmarkVisitRepository;
import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import com.goodspace.runny.domain.quest.repository.UserQuestRepository;
import com.goodspace.runny.domain.user.entity.Gender;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 업적 서비스 (최종 확정안). 러닝 완료/친구 수락/크루 가입/꾸미기/퀘스트 완료 이벤트로 달성을 판정하고(달성 기록만),
 * 보상은 수령 버튼(claim API)으로 지급한다. 횟수/연속형 업적은 achievement_progress 카운터로 관리한다.
 * 여성은 견종 해금 업적의 페이스 기준 +1분(60초) 보정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    // 견종 업적 페이스 기준 (초/km). 여성은 +60초
    private static final long BORDER_COLLIE_PACE_SEC = 360;   // 6:00/km
    private static final double BORDER_COLLIE_DISTANCE_KM = 20;
    private static final long GREYHOUND_PACE_SEC = 210;       // 3:30/km
    private static final double GREYHOUND_DISTANCE_KM = 5;
    private static final long FEMALE_PACE_BONUS_SEC = 60;
    // 풀냄새 맡기 (슬로우 조깅) 기준: 평균 페이스 9:00/km 이상 + 20분 이상
    private static final long GRASS_SNIFF_PACE_SEC = 540;
    private static final long GRASS_SNIFF_DURATION_SEC = 20 * 60;
    private static final int LANDMARK_TARGET_COUNT = 3;
    // 치타보다 빠른 러니: 5km 이상 + 평균 페이스 5:00/km(300초) 이하
    private static final long CHEETAH_PACE_SEC = 300;
    private static final double CHEETAH_DISTANCE_KM = 5;
    // 야간/모닝 순찰대원: 2km 이상 러닝 5회 (야간: 21시 이후 시작 / 모닝: 10시 이전 시작, KST)
    private static final double PATROL_DISTANCE_KM = 2;
    private static final int PATROL_TARGET_COUNT = 5;
    private static final int NIGHT_HOUR_FROM = 21;
    private static final int MORNING_HOUR_BEFORE = 10;
    // 전담 사진 작가: 꾸미기 30회
    private static final int PHOTOGRAPHER_TARGET_COUNT = 30;
    // 일일 퀘스트 올클리어: 10일
    private static final int ALL_CLEAR_TARGET_DAYS = 10;
    // 장거리 산책 마스터: 10km 이상 러닝 3회
    private static final double LONG_RUN_KM = 10;
    private static final int LONG_RUN_TARGET_COUNT = 3;
    // 철인 훈련: 3일 연속 5km 이상
    private static final double IRON_RUN_KM = 5;
    private static final int IRON_TARGET_DAYS = 3;
    // 미션은 이제 쉽다개!: 총 완료 퀘스트 100개
    private static final long QUEST_MASTER_TARGET = 100;

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserLandmarkVisitRepository userLandmarkVisitRepository;
    private final AchievementProgressRepository achievementProgressRepository;
    private final UserQuestRepository userQuestRepository;
    private final UserRepository userRepository;
    private final CoinService coinService;

    /** 러닝 완료 판정 입력 - 러닝 도메인에서 전달. totalDistanceKm은 이번 러닝을 포함한 누적 거리 */
    public record RunningResult(
            double distanceKm,
            long durationSec,
            long longestNonstopSec,
            long avgPaceSec,
            Long yesterdayAvgPaceSec,
            double totalDistanceKm,
            LocalDateTime startedAt,
            List<Long> visitedLandmarkIds
    ) {
    }

    /**
     * 러닝 완료 시 업적 일괄 판정. 방문 랜드마크를 먼저 기록한 뒤 각 조건을 검사하고,
     * 새로 달성된 업적 목록(알림성 응답 데이터)을 반환한다. 보상 지급은 수령 API에서 수행.
     */
    @Transactional
    public List<AchievementDto.AchievedItem> evaluateRunning(Long userId, RunningResult result) {
        recordLandmarkVisits(userId, result.visitedLandmarkIds());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
        long femaleBonus = user.getGender() == Gender.F ? FEMALE_PACE_BONUS_SEC : 0;

        List<AchievementDto.AchievedItem> achieved = new ArrayList<>();
        // 예방접종 하러 가는 길: 1km 달리기
        if (result.distanceKm() >= 1) {
            achieve(userId, AchievementSeeder.VACCINATION, achieved);
        }
        // 인내심 기르기: 10분 무정지
        if (result.longestNonstopSec() >= 10 * 60) {
            achieve(userId, AchievementSeeder.PATIENCE, achieved);
        }
        // 장거리 산책하기: 3km 이상
        if (result.distanceKm() >= 3) {
            achieve(userId, AchievementSeeder.LONG_WALK, achieved);
        }
        // 넘치는 에너지 방출: 어제보다 평균 페이스 향상 (어제 기록이 없으면 미판정)
        if (result.yesterdayAvgPaceSec() != null && result.avgPaceSec() < result.yesterdayAvgPaceSec()) {
            achieve(userId, AchievementSeeder.ENERGY_BURST, achieved);
        }
        // 풀냄새 맡기: 평균 페이스 9:00/km 이상(느림) + 20분 이상
        if (result.avgPaceSec() >= GRASS_SNIFF_PACE_SEC && result.durationSec() >= GRASS_SNIFF_DURATION_SEC) {
            achieve(userId, AchievementSeeder.GRASS_SNIFF, achieved);
        }
        // 치타보다 빠른 러니: 5km 이상 + 5:00/km 이하
        if (result.distanceKm() >= CHEETAH_DISTANCE_KM && result.avgPaceSec() <= CHEETAH_PACE_SEC) {
            achieve(userId, AchievementSeeder.CHEETAH_RUNNY, achieved);
        }
        // 동네 도장 깨기: 누적 랜드마크 3곳 방문
        if (userLandmarkVisitRepository.countByUserId(userId) >= LANDMARK_TARGET_COUNT) {
            achieve(userId, AchievementSeeder.LANDMARK_MASTER, achieved);
        }
        // 야간/모닝 순찰대원: 2km 이상 러닝 5회 (시작 시각 기준)
        int startHour = result.startedAt().getHour();
        if (result.distanceKm() >= PATROL_DISTANCE_KM && startHour >= NIGHT_HOUR_FROM) {
            countAndAchieve(userId, AchievementSeeder.NIGHT_PATROL, PATROL_TARGET_COUNT, achieved);
        }
        if (result.distanceKm() >= PATROL_DISTANCE_KM && startHour < MORNING_HOUR_BEFORE) {
            countAndAchieve(userId, AchievementSeeder.MORNING_PATROL, PATROL_TARGET_COUNT, achieved);
        }
        // 장거리 산책 마스터: 10km 이상 러닝 3회
        if (result.distanceKm() >= LONG_RUN_KM) {
            countAndAchieve(userId, AchievementSeeder.LONG_RUN_MASTER, LONG_RUN_TARGET_COUNT, achieved);
        }
        // 철인 훈련: 3일 연속 5km 이상 (러닝 일자 기준 연속 판정, 하루 여러 번은 1일로 처리)
        if (result.distanceKm() >= IRON_RUN_KM) {
            String dayKey = result.startedAt().toLocalDate().toString();
            String yesterdayKey = result.startedAt().toLocalDate().minusDays(1).toString();
            AchievementProgress progress = progressOf(userId, AchievementSeeder.IRON_TRAINING);
            progress.continueOrResetStreak(dayKey, yesterdayKey);
            if (progress.getProgress() >= IRON_TARGET_DAYS) {
                achieve(userId, AchievementSeeder.IRON_TRAINING, achieved);
            }
        }
        // 벌써 이만큼이나?: 누적 거리 단계별 (50/100/200km ...)
        for (int tierKm : AchievementSeeder.MILEAGE_TIERS) {
            if (result.totalDistanceKm() >= tierKm) {
                achieve(userId, AchievementSeeder.mileageCode(tierKm), achieved);
            }
        }
        // 보더콜리: 6:00/km 이내(여성 7:00) + 20km 완주
        if (result.avgPaceSec() <= BORDER_COLLIE_PACE_SEC + femaleBonus
                && result.distanceKm() >= BORDER_COLLIE_DISTANCE_KM) {
            achieve(userId, AchievementSeeder.BORDER_COLLIE, achieved);
        }
        // 그레이하운드: 3:30/km 이내(여성 4:30) + 5km 완주
        if (result.avgPaceSec() <= GREYHOUND_PACE_SEC + femaleBonus
                && result.distanceKm() >= GREYHOUND_DISTANCE_KM) {
            achieve(userId, AchievementSeeder.GREYHOUND, achieved);
        }
        return achieved;
    }

    /** 친구 수락 이벤트 판정 - 친구 사귀기 업적 (수락자/요청자 양쪽 모두, 친구 도메인 훅에서 호출) */
    @Transactional
    public void onFriendAccepted(Long requesterId, Long receiverId) {
        achieve(requesterId, AchievementSeeder.MAKE_FRIEND, new ArrayList<>());
        achieve(receiverId, AchievementSeeder.MAKE_FRIEND, new ArrayList<>());
    }

    /** 크루 가입 이벤트 판정 - 친구들과 달리기 업적 (크루 생성/가입 승인 시 크루 도메인 훅에서 호출) */
    @Transactional
    public void onCrewJoined(Long userId) {
        achieve(userId, AchievementSeeder.CREW_JOIN, new ArrayList<>());
    }

    /** 꾸미기 완료 이벤트 판정 - 전담 사진 작가 업적 카운트 (퀘스트 도메인 꾸미기 이벤트에서 호출) */
    @Transactional
    public void onDecorated(Long userId) {
        countAndAchieve(userId, AchievementSeeder.PHOTOGRAPHER, PHOTOGRAPHER_TARGET_COUNT, new ArrayList<>());
    }

    /**
     * 퀘스트 진행도 변경 후 판정 - 총 완료 퀘스트 100개(미션은 이제 쉽다개!)와
     * 일일 퀘스트 올클리어 10일(하루 1회만 카운트). 퀘스트 도메인이 진행도 갱신 직후 호출한다.
     */
    @Transactional
    public void onQuestUpdated(Long userId, String todayKey) {
        // 총 완료 퀘스트 100개
        if (userQuestRepository.countByUserIdAndCompletedTrue(userId) >= QUEST_MASTER_TARGET) {
            achieve(userId, AchievementSeeder.QUEST_MASTER, new ArrayList<>());
        }
        // 오늘의 일일 퀘스트(고정 3 + 랜덤 2) 전부 완료 시 올클리어 일수 +1 (같은 날 중복 카운트 방지)
        List<UserQuest> today = userQuestRepository.findByUserIdAndPeriodKey(userId, todayKey);
        boolean allClear = !today.isEmpty() && today.stream().allMatch(UserQuest::isCompleted);
        if (allClear && !isAchieved(userId, AchievementSeeder.DAILY_ALL_CLEAR)) {
            AchievementProgress progress = progressOf(userId, AchievementSeeder.DAILY_ALL_CLEAR);
            progress.incrementOncePerPeriod(todayKey);
            if (progress.getProgress() >= ALL_CLEAR_TARGET_DAYS) {
                achieve(userId, AchievementSeeder.DAILY_ALL_CLEAR, new ArrayList<>());
            }
        }
    }

    /**
     * 업적 보상 수령 - 조건부 UPDATE(claimed=false -> true)로 수령을 선점한 요청만 보상을 지급한다.
     * 코인은 CoinService.add, 견종 해금은 claimed 처리 자체가 해금(AchievementUnlockChecker 판정 기준).
     */
    @Transactional
    public AchievementDto.ClaimResponse claim(Long userId, Long achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_003));
        userAchievementRepository.findByUserIdAndAchievementId(userId, achievementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_001));

        // 수령 선점 - 이미 수령됐거나 동시 요청에 밀리면 영향 행 0 (중복 지급 원천 차단)
        if (userAchievementRepository.claimIfNotClaimed(userId, achievementId) == 0) {
            throw new BusinessException(ErrorCode.ACHIEVEMENT_002);
        }

        // 코인성 보상은 dog_change_log 기록 없음 (경험치 아님)
        if (achievement.getRewardType() == RewardType.COIN && achievement.getRewardCoin() > 0) {
            coinService.add(userId, achievement.getRewardCoin(), CoinTransactionType.ACHIEVEMENT, achievement.getId());
        }
        return new AchievementDto.ClaimResponse(
                achievement.getRewardType(), achievement.getRewardCoin(), achievement.getRewardBreedId());
    }

    /** 업적 목록 - 아이콘/달성/수령 여부 + 달성률 */
    @Transactional(readOnly = true)
    public AchievementDto.ListResponse getAchievements(Long userId) {
        List<Achievement> all = achievementRepository.findAllByOrderByIdAsc();
        Map<Long, UserAchievement> mine = new HashMap<>();
        userAchievementRepository.findByUserId(userId)
                .forEach(ua -> mine.put(ua.getAchievement().getId(), ua));

        List<AchievementDto.Item> items = all.stream()
                .map(achievement -> AchievementDto.Item.of(achievement, mine.get(achievement.getId())))
                .toList();
        int achievedCount = mine.size();
        double rate = all.isEmpty() ? 0 : Math.round(achievedCount * 1000.0 / all.size()) / 10.0;
        return new AchievementDto.ListResponse(items, achievedCount, all.size(), rate);
    }

    /** 카운트형 업적 공통 - 카운터 +1 후 목표 도달 시 달성 처리 */
    private void countAndAchieve(Long userId, String code, int target, List<AchievementDto.AchievedItem> achieved) {
        // 이미 달성한 업적은 카운터를 더 올릴 필요 없음
        if (isAchieved(userId, code)) {
            return;
        }
        AchievementProgress progress = progressOf(userId, code);
        progress.increment();
        if (progress.getProgress() >= target) {
            achieve(userId, code, achieved);
        }
    }

    /** 진행 카운터 조회/생성 - (user, code) UNIQUE 경쟁 시 기존 행 재조회 */
    private AchievementProgress progressOf(Long userId, String code) {
        return achievementProgressRepository.findByUserIdAndCode(userId, code)
                .orElseGet(() -> {
                    try {
                        return achievementProgressRepository.saveAndFlush(new AchievementProgress(userId, code));
                    } catch (DataIntegrityViolationException e) {
                        return achievementProgressRepository.findByUserIdAndCode(userId, code)
                                .orElseThrow(() -> e);
                    }
                });
    }

    /** 달성 여부 조회 */
    private boolean isAchieved(Long userId, String code) {
        return achievementRepository.findByCode(code)
                .map(achievement -> userAchievementRepository
                        .existsByUserIdAndAchievementId(userId, achievement.getId()))
                .orElse(false);
    }

    /**
     * 달성 처리 공통 - 1회성이므로 이미 달성했으면 무시. 동시 달성 판정 경쟁은
     * (user, achievement) UNIQUE 위반을 잡아 조용히 무시한다 (먼저 기록한 요청이 유효).
     */
    private void achieve(Long userId, String code, List<AchievementDto.AchievedItem> achieved) {
        Achievement achievement = achievementRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_003));
        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
            return;
        }
        try {
            userAchievementRepository.saveAndFlush(new UserAchievement(userId, achievement));
        } catch (DataIntegrityViolationException e) {
            log.debug("업적 동시 달성 경쟁 - 기존 기록 유지: user={}, code={}", userId, code);
            return;
        }
        achieved.add(new AchievementDto.AchievedItem(
                achievement.getId(), achievement.getCode(), achievement.getTitle(), achievement.getImageUrl()));
    }

    /** 방문 랜드마크 기록 - 최초 방문만 저장 */
    private void recordLandmarkVisits(Long userId, List<Long> landmarkIds) {
        if (landmarkIds == null) {
            return;
        }
        landmarkIds.stream().distinct()
                .filter(landmarkId -> !userLandmarkVisitRepository.existsByUserIdAndLandmarkId(userId, landmarkId))
                .forEach(landmarkId ->
                        userLandmarkVisitRepository.save(new UserLandmarkVisit(userId, landmarkId)));
    }
}

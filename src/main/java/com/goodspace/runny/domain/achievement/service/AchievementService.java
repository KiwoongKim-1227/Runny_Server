package com.goodspace.runny.domain.achievement.service;

import com.goodspace.runny.domain.achievement.dto.AchievementDto;
import com.goodspace.runny.domain.achievement.entity.Achievement;
import com.goodspace.runny.domain.achievement.entity.RewardType;
import com.goodspace.runny.domain.achievement.entity.UserAchievement;
import com.goodspace.runny.domain.achievement.entity.UserLandmarkVisit;
import com.goodspace.runny.domain.achievement.repository.AchievementRepository;
import com.goodspace.runny.domain.achievement.repository.UserAchievementRepository;
import com.goodspace.runny.domain.achievement.repository.UserLandmarkVisitRepository;
import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.user.entity.Gender;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 업적 서비스. 러닝 완료 데이터와 친구 수락 이벤트로 달성을 판정하고(달성 기록만),
 * 보상은 수령 버튼(claim API)으로 지급한다. 여성은 견종 업적 페이스 기준 +1분(60초) 보정.
 */
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

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserLandmarkVisitRepository userLandmarkVisitRepository;
    private final UserRepository userRepository;
    private final CoinService coinService;

    /** 러닝 완료 판정 입력 - 러닝 도메인(9단계)에서 전달 */
    public record RunningResult(
            double distanceKm,
            long durationSec,
            long longestNonstopSec,
            long avgPaceSec,
            Long yesterdayAvgPaceSec,
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
        // 동네 대장 도장 깨기: 누적 랜드마크 3곳 방문
        if (userLandmarkVisitRepository.countByUserId(userId) >= LANDMARK_TARGET_COUNT) {
            achieve(userId, AchievementSeeder.LANDMARK_MASTER, achieved);
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

    /** 친구 수락 이벤트 판정 - 친구 사귀기 업적 (수락자/요청자 양쪽 모두, 6단계 훅에서 호출) */
    @Transactional
    public void onFriendAccepted(Long requesterId, Long receiverId) {
        achieve(requesterId, AchievementSeeder.MAKE_FRIEND, new ArrayList<>());
        achieve(receiverId, AchievementSeeder.MAKE_FRIEND, new ArrayList<>());
    }

    /** 업적 보상 수령 - 코인은 CoinService.add, 견종 해금은 claimed 처리 자체가 해금(AchievementUnlockChecker 판정 기준) */
    @Transactional
    public AchievementDto.ClaimResponse claim(Long userId, Long achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_003));
        UserAchievement userAchievement = userAchievementRepository
                .findByUserIdAndAchievementId(userId, achievementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_001));
        if (userAchievement.isClaimed()) {
            throw new BusinessException(ErrorCode.ACHIEVEMENT_002);
        }
        userAchievement.claim();

        // 코인성 보상은 dog_change_log 기록 없음 (경험치 아님, 문서 4.F)
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

    /** 달성 처리 공통 - 1회성이므로 이미 달성했으면 무시. 새로 달성한 경우 응답 목록에 추가 */
    private void achieve(Long userId, String code, List<AchievementDto.AchievedItem> achieved) {
        Achievement achievement = achievementRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACHIEVEMENT_003));
        if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
            return;
        }
        userAchievementRepository.save(new UserAchievement(userId, achievement));
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

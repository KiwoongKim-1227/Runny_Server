package com.goodspace.runny.domain.achievement.service;

import com.goodspace.runny.domain.achievement.entity.Achievement;
import com.goodspace.runny.domain.achievement.entity.Landmark;
import com.goodspace.runny.domain.achievement.entity.RewardType;
import com.goodspace.runny.domain.achievement.repository.AchievementRepository;
import com.goodspace.runny.domain.achievement.repository.LandmarkRepository;
import com.goodspace.runny.domain.dog.repository.DogBreedRepository;
import com.goodspace.runny.domain.dog.service.DogBreedSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 업적/랜드마크 시드 등록기 (문서 4.F 표 9종). 견종 해금형 업적은 dog_breed와 코드로 매칭해
 * achievement.reward_breed_id와 dog_breed.unlock_achievement_id를 양방향 백필한다.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class AchievementSeeder implements CommandLineRunner {

    // 업적 코드 - 판정 로직(AchievementService)과 견종 해금 매칭에 사용
    public static final String VACCINATION = "VACCINATION_RUN";
    public static final String PATIENCE = "PATIENCE";
    public static final String LONG_WALK = "LONG_WALK";
    public static final String ENERGY_BURST = "ENERGY_BURST";
    public static final String MAKE_FRIEND = "MAKE_FRIEND";
    public static final String LANDMARK_MASTER = "LANDMARK_MASTER";
    public static final String GRASS_SNIFF = "GRASS_SNIFF";
    public static final String BORDER_COLLIE = DogBreedSeeder.ACHIEVEMENT_BORDER_COLLIE;
    public static final String GREYHOUND = DogBreedSeeder.ACHIEVEMENT_GREYHOUND;

    private final AchievementRepository achievementRepository;
    private final LandmarkRepository landmarkRepository;
    private final DogBreedRepository dogBreedRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (achievementRepository.count() > 0) {
            return;
        }
        // 견종 해금형 업적의 대상 견종 조회 (3단계 DogBreedSeeder가 등록한 코드로 매칭)
        Map<String, Long> breedIdByCode = new java.util.HashMap<>();
        dogBreedRepository.findAll().stream()
                .filter(breed -> breed.getUnlockAchievementCode() != null)
                .forEach(breed -> breedIdByCode.put(breed.getUnlockAchievementCode(), breed.getId()));

        List<Achievement> achievements = achievementRepository.saveAll(List.of(
                coin(VACCINATION, "예방접종 하러 가는 길", "1km 달리기", 100),
                coin(PATIENCE, "인내심 기르기", "10분 동안 멈추지 않고 달리기", 100),
                coin(LONG_WALK, "장거리 산책하기", "3km 이상 달리기", 150),
                coin(ENERGY_BURST, "넘치는 에너지 방출", "어제보다 평균 페이스를 높여서 달리기", 150),
                coin(MAKE_FRIEND, "친구 사귀기", "다른 유저와 친구 맺기", 100),
                coin(LANDMARK_MASTER, "동네 대장 도장 깨기", "지정된 랜드마크 3곳 방문", 300),
                coin(GRASS_SNIFF, "풀냄새 맡기", "슬로우 조깅 (평균 페이스 9:00/km 이상, 20분 이상)", 100),
                dogUnlock(BORDER_COLLIE, "보더콜리", "평균 페이스 6:00/km 이내로 20km 완주 (여성은 7:00/km)",
                        breedIdByCode.get(BORDER_COLLIE)),
                dogUnlock(GREYHOUND, "그레이하운드", "평균 페이스 3:30/km로 5km 완주 (여성은 4:30/km)",
                        breedIdByCode.get(GREYHOUND))
        ));

        // dog_breed.unlock_achievement_id 백필 (3단계에서 예고한 FK 연결)
        achievements.stream()
                .filter(achievement -> achievement.getRewardType() == RewardType.DOG_UNLOCK)
                .forEach(achievement -> dogBreedRepository.findAll().stream()
                        .filter(breed -> achievement.getCode().equals(breed.getUnlockAchievementCode()))
                        .forEach(breed -> breed.linkUnlockAchievement(achievement.getId())));

        // 랜드마크 시드 (동네 대장 업적용, 좌표/반경은 운영 시 조정)
        if (landmarkRepository.count() == 0) {
            landmarkRepository.saveAll(List.of(
                    new Landmark("일산호수공원", 37.6584, 126.7654, 300),
                    new Landmark("한강공원 반포지구", 37.5100, 126.9958, 300),
                    new Landmark("올림픽공원", 37.5202, 127.1214, 300)
            ));
        }
        log.info("업적 마스터 시드 9종 + 랜드마크 3곳 등록 완료");
    }

    /** 코인 보상 업적 헬퍼 */
    private Achievement coin(String code, String title, String description, int rewardCoin) {
        return Achievement.builder()
                .code(code).title(title).description(description)
                .imageUrl(imageUrl(code))
                .rewardType(RewardType.COIN).rewardCoin(rewardCoin)
                .build();
    }

    /** 견종 해금 보상 업적 헬퍼 */
    private Achievement dogUnlock(String code, String title, String description, Long breedId) {
        return Achievement.builder()
                .code(code).title(title).description(description)
                .imageUrl(imageUrl(code))
                .rewardType(RewardType.DOG_UNLOCK).rewardCoin(0).rewardBreedId(breedId)
                .build();
    }

    /** S3 정적 리소스 경로 규칙 (운영자 업로드) */
    private String imageUrl(String code) {
        return "https://runny-assets.s3.ap-northeast-2.amazonaws.com/achievement/" + code.toLowerCase() + ".png";
    }
}

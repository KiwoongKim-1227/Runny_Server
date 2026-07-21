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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 업적/랜드마크 시드 등록기 (최종 확정안 - 코인 업적 18종 + 견종 해금 2종).
 * 누적 거리 업적(벌써 이만큼이나?)은 단계별(50/100/200km) 개별 업적으로 등록하며 지급 코인 = 단계 km x 2.
 * 견종 해금(보더콜리/그레이하운드)은 확정안 목록에 없지만 견종 시스템이 참조하므로 유지한다.
 * 기존 데이터가 있으면 건너뛰므로, 확정안 반영 시 achievement / user_achievement 테이블을 비운 뒤 재기동해야 한다.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class AchievementSeeder implements CommandLineRunner {

    // 업적 코드 - 판정 로직(AchievementService)과 견종 해금 매칭에 사용
    public static final String VACCINATION = "VACCINATION_RUN";
    public static final String PATIENCE = "PATIENCE";
    public static final String MAKE_FRIEND = "MAKE_FRIEND";
    public static final String CREW_JOIN = "CREW_JOIN";
    public static final String LANDMARK_MASTER = "LANDMARK_MASTER";
    public static final String LONG_WALK = "LONG_WALK";
    public static final String ENERGY_BURST = "ENERGY_BURST";
    public static final String NIGHT_PATROL = "NIGHT_PATROL";
    public static final String MORNING_PATROL = "MORNING_PATROL";
    public static final String PHOTOGRAPHER = "PHOTOGRAPHER";
    public static final String DAILY_ALL_CLEAR = "DAILY_ALL_CLEAR";
    public static final String LONG_RUN_MASTER = "LONG_RUN_MASTER";
    public static final String IRON_TRAINING = "IRON_TRAINING";
    public static final String QUEST_MASTER = "QUEST_MASTER";
    public static final String CHEETAH_RUNNY = "CHEETAH_RUNNY";
    public static final String GRASS_SNIFF = "GRASS_SNIFF";
    public static final String BORDER_COLLIE = DogBreedSeeder.ACHIEVEMENT_BORDER_COLLIE;
    public static final String GREYHOUND = DogBreedSeeder.ACHIEVEMENT_GREYHOUND;

    // 누적 거리 단계 (km). 확장 시 단계만 추가하면 된다 (지급 코인 = km x 2)
    public static final int[] MILEAGE_TIERS = {50, 100, 200};

    /** 누적 거리 단계 업적 코드 (예: MILEAGE_50) */
    public static String mileageCode(int tierKm) {
        return "MILEAGE_" + tierKm;
    }

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

        List<Achievement> seeds = new ArrayList<>(List.of(
                coin(VACCINATION, "예방접종 하러 가는 길", "1km 달리기", 50),
                coin(PATIENCE, "인내심 기르기", "10분 동안 멈추지 않고 달리기", 50),
                coin(MAKE_FRIEND, "친구 사귀기", "다른 유저와 친구 맺기", 50),
                coin(CREW_JOIN, "친구들과 달리기", "크루 가입하기", 50),
                coin(LANDMARK_MASTER, "동네 도장 깨기", "지정된 랜드마크 3곳 방문하기", 100),
                coin(LONG_WALK, "장거리 산책하기", "3km 이상 달리기", 100),
                coin(ENERGY_BURST, "넘치는 에너지 방출", "페이스를 어제보다 높여서 달리기", 150),
                coin(NIGHT_PATROL, "야간 순찰대원", "밤 9시 이후에 2km 이상 달리기 5회 달성", 150),
                coin(MORNING_PATROL, "모닝 순찰대원", "아침 10시 이전에 2km 이상 달리기 5회 달성", 150),
                coin(PHOTOGRAPHER, "전담 사진 작가", "러닝 완료 후 꾸미기 기능 30회 사용하기", 150),
                coin(DAILY_ALL_CLEAR, "일일 퀘스트 올클리어", "일일 퀘스트를 모두 완료한 날 10일 달성", 150),
                coin(GRASS_SNIFF, "풀냄새 맡기", "슬로우 조깅 (평균 페이스 9:00/km 이상, 20분 이상)", 150),
                coin(LONG_RUN_MASTER, "장거리 산책 마스터", "10km 이상 러닝 3회 이상 완료", 200),
                coin(IRON_TRAINING, "철인 훈련", "3일 연속으로 5km 이상 달리기", 200),
                coin(QUEST_MASTER, "미션은 이제 쉽다개!", "총 완료 퀘스트 100개 달성", 200),
                coin(CHEETAH_RUNNY, "치타보다 빠른 러니", "5km 이상 러닝 시 5분 이하 페이스 기록", 250)
        ));
        // 누적 거리 단계별 업적 - 지급 코인 = 단계 km x 2
        for (int tierKm : MILEAGE_TIERS) {
            seeds.add(coin(mileageCode(tierKm), "벌써 이만큼이나? (" + tierKm + "km)",
                    "누적 거리 " + tierKm + "km 달성", tierKm * 2));
        }
        // 견종 해금형 (확정안 목록 외 - 견종 시스템 유지용)
        seeds.add(dogUnlock(BORDER_COLLIE, "보더콜리", "평균 페이스 6:00/km 이내로 20km 완주 (여성은 7:00/km)",
                breedIdByCode.get(BORDER_COLLIE)));
        seeds.add(dogUnlock(GREYHOUND, "그레이하운드", "평균 페이스 3:30/km로 5km 완주 (여성은 4:30/km)",
                breedIdByCode.get(GREYHOUND)));

        List<Achievement> achievements = achievementRepository.saveAll(seeds);

        // dog_breed.unlock_achievement_id 백필 (3단계에서 예고한 FK 연결)
        achievements.stream()
                .filter(achievement -> achievement.getRewardType() == RewardType.DOG_UNLOCK)
                .forEach(achievement -> dogBreedRepository.findAll().stream()
                        .filter(breed -> achievement.getCode().equals(breed.getUnlockAchievementCode()))
                        .forEach(breed -> breed.linkUnlockAchievement(achievement.getId())));

        // 랜드마크 시드 (동네 도장 깨기 업적용, 좌표/반경은 운영 시 조정)
        if (landmarkRepository.count() == 0) {
            landmarkRepository.saveAll(List.of(
                    new Landmark("일산호수공원", 37.6584, 126.7654, 300),
                    new Landmark("한강공원 반포지구", 37.5100, 126.9958, 300),
                    new Landmark("올림픽공원", 37.5202, 127.1214, 300)
            ));
        }
        log.info("업적 마스터 시드 {}종 + 랜드마크 3곳 등록 완료", achievements.size());
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

package com.goodspace.runny.domain.dog.dto;

import com.goodspace.runny.domain.dog.entity.BreedGrade;
import com.goodspace.runny.domain.dog.entity.DogBreed;
import com.goodspace.runny.domain.dog.entity.GrowthStage;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.service.DogChangeLogService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 강아지 도메인 요청/응답 DTO 모음.
 */
public final class DogDto {

    private DogDto() {
    }

    /** 강아지 생성 요청 (온보딩 첫 선택 / 입양 공용) */
    public record CreateRequest(
            @NotNull Long breedId,
            @NotBlank String name
    ) {
    }

    /** 활성 강아지 전환 요청 */
    public record ActiveRequest(
            @NotNull Long dogId
    ) {
    }

    /** 견종 목록 응답 - 온보딩/입양 화면 공용 */
    public record BreedResponse(
            Long breedId,
            String name,
            String description,
            BreedGrade grade,
            int price,
            String modelUrl,
            boolean owned,
            boolean achievementLocked,
            boolean unlocked
    ) {
        /** owned: 보유 여부 / achievementLocked: 업적 해금형 여부 / unlocked: (해금형일 때) 해금 여부 */
        public static BreedResponse of(DogBreed breed, boolean owned, boolean unlocked) {
            return new BreedResponse(
                    breed.getId(), breed.getName(), breed.getDescription(),
                    breed.getGrade(), breed.getPrice(), breed.getModelUrl(),
                    owned, breed.requiresAchievementUnlock(), unlocked
            );
        }
    }

    /** 보유 강아지 목록 항목 - 종 변경 화면용 (설정한 이름으로 표시) */
    public record MyDogResponse(
            Long dogId,
            String name,
            String breedName,
            String breedModelUrl,
            int level,
            boolean active
    ) {
        public static MyDogResponse of(UserDog dog, boolean active) {
            return new MyDogResponse(
                    dog.getId(), dog.getName(),
                    dog.getBreed().getName(), dog.getBreed().getModelUrl(),
                    dog.getLevel(), active
            );
        }
    }

    /** 강아지 생성 응답 */
    public record CreateResponse(
            Long dogId,
            String name,
            String breedName,
            boolean onboardingCompleted
    ) {
    }

    /** 펫 프로필 응답 - 미확인 변화 요약 포함 (조회 시 확인 처리됨) */
    public record ProfileResponse(
            Long dogId,
            String name,
            String breedName,
            String breedDescription,
            String breedModelUrl,
            int level,
            GrowthStage growthStage,
            int exp,
            int expToNextLevel,
            int stamina,
            int endurance,
            int speed,
            DogChangeLogService.ChangeSummary changeSummary
    ) {
        public static ProfileResponse of(UserDog dog, DogChangeLogService.ChangeSummary summary) {
            return new ProfileResponse(
                    dog.getId(), dog.getName(),
                    dog.getBreed().getName(), dog.getBreed().getDescription(), dog.getBreed().getModelUrl(),
                    dog.getLevel(), dog.growthStage(), dog.getExp(), dog.expToNextLevel(),
                    dog.getStamina(), dog.getEndurance(), dog.getSpeed(),
                    summary
            );
        }
    }
}

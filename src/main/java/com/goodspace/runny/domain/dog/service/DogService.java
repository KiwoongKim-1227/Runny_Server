package com.goodspace.runny.domain.dog.service;

import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
import com.goodspace.runny.domain.dog.dto.DogDto;
import com.goodspace.runny.domain.dog.entity.BreedGrade;
import com.goodspace.runny.domain.dog.entity.DogBreed;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.repository.DogBreedRepository;
import com.goodspace.runny.domain.dog.repository.UserDogRepository;
import com.goodspace.runny.domain.user.entity.OnboardingStatus;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.util.ProfanityFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 강아지 서비스. 견종 목록, 생성(온보딩 무료/입양 코인 차감), 활성 강아지 전환, 보유 목록, 펫 프로필을 담당한다.
 */
@Service
@RequiredArgsConstructor
public class DogService {

    // 강아지 이름 규칙: 1~7자, 한글/영문/숫자만 (특수문자 불가). 중복은 허용
    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]{1,7}$");

    private final DogBreedRepository dogBreedRepository;
    private final UserDogRepository userDogRepository;
    private final UserRepository userRepository;
    private final CoinService coinService;
    private final ProfanityFilter profanityFilter;
    private final DogChangeLogService dogChangeLogService;
    private final AchievementUnlockChecker achievementUnlockChecker;

    /** 견종 목록 조회 - 등급/가격/소개/보유 여부/해금 여부 포함 (온보딩/입양 공용) */
    @Transactional(readOnly = true)
    public List<DogDto.BreedResponse> getBreeds(Long userId) {
        Set<Long> ownedBreedIds = userDogRepository.findByUserId(userId).stream()
                .map(dog -> dog.getBreed().getId())
                .collect(Collectors.toSet());
        return dogBreedRepository.findAllByOrderByIdAsc().stream()
                .map(breed -> DogDto.BreedResponse.of(
                        breed,
                        ownedBreedIds.contains(breed.getId()),
                        breed.requiresAchievementUnlock()
                                && achievementUnlockChecker.isUnlocked(userId, breed.getUnlockAchievementCode())))
                .toList();
    }

    /**
     * 강아지 생성. 온보딩 첫 생성(DOG_REQUIRED 상태)은 무료(일반 견종만)이며 활성 지정 + 온보딩 완료 처리.
     * 이후 입양은 검증(중복 보유/미해금 레어/잔액) 후 코인 차감과 함께 생성한다. 생성과 동시에 이름 설정.
     */
    @Transactional
    public DogDto.CreateResponse createDog(Long userId, DogDto.CreateRequest request) {
        User user = findUser(userId);
        DogBreed breed = dogBreedRepository.findById(request.breedId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_003));
        validateName(request.name());

        boolean onboarding = user.getOnboardingStatus() == OnboardingStatus.DOG_REQUIRED;
        if (user.getOnboardingStatus() == OnboardingStatus.PROFILE_REQUIRED) {
            throw new BusinessException(ErrorCode.DOG_008);
        }

        if (onboarding) {
            // 온보딩 첫 선택: 일반 견종만 허용, 무료
            if (breed.getGrade() != BreedGrade.NORMAL) {
                throw new BusinessException(ErrorCode.DOG_006);
            }
        } else {
            // 입양: 중복 보유 / 미해금 레어 검증 후 가격만큼 코인 차감
            if (userDogRepository.existsByUserIdAndBreedId(userId, breed.getId())) {
                throw new BusinessException(ErrorCode.DOG_004);
            }
            if (breed.requiresAchievementUnlock()
                    && !achievementUnlockChecker.isUnlocked(userId, breed.getUnlockAchievementCode())) {
                throw new BusinessException(ErrorCode.DOG_005);
            }
            // 업적 해금형은 해금 후 무료(price=0), 그 외는 코인 차감 + 원장 기록
            if (breed.getPrice() > 0) {
                coinService.deduct(userId, breed.getPrice(), CoinTransactionType.DOG_PURCHASE, breed.getId());
            }
        }

        UserDog dog = userDogRepository.save(new UserDog(userId, breed, request.name()));

        if (onboarding) {
            user.completeOnboarding(dog.getId());
        }
        return new DogDto.CreateResponse(dog.getId(), dog.getName(), breed.getName(), onboarding);
    }

    /** 보유 강아지 목록 - 설정한 이름으로 표시 (종 변경 화면용) */
    @Transactional(readOnly = true)
    public List<DogDto.MyDogResponse> getMyDogs(Long userId) {
        Long activeDogId = findUser(userId).getActiveDogId();
        return userDogRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(dog -> DogDto.MyDogResponse.of(dog, dog.getId().equals(activeDogId)))
                .toList();
    }

    /** 활성 강아지 전환 - 착용 코디는 강아지별로 유지되므로 참조만 변경한다 */
    @Transactional
    public void changeActiveDog(Long userId, Long dogId) {
        User user = findUser(userId);
        UserDog dog = userDogRepository.findByIdAndUserId(dogId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_007));
        user.changeActiveDog(dog.getId());
    }

    /** 펫 프로필 조회 - 미확인 변화 요약을 함께 반환하고 해당 로그를 일괄 확인 처리한다 */
    @Transactional
    public DogDto.ProfileResponse getActiveProfile(Long userId) {
        UserDog dog = findActiveDog(userId);
        DogChangeLogService.ChangeSummary summary = dogChangeLogService.summarizeAndMarkSeen(dog.getId());
        return DogDto.ProfileResponse.of(dog, summary);
    }

    /** 활성 강아지의 미확인 변경 존재 여부 - 놀이터 hasDogProfileBadge용 (친구/놀이터 단계에서 호출 예정) */
    @Transactional(readOnly = true)
    public boolean hasUnseenChangesForActiveDog(Long userId) {
        Long activeDogId = findUser(userId).getActiveDogId();
        return activeDogId != null && dogChangeLogService.hasUnseenChanges(activeDogId);
    }

    /** 활성 강아지 조회 - 드레스룸/상점 등 다른 도메인에서 재사용하는 공개 메서드 */
    @Transactional(readOnly = true)
    public UserDog getActiveDog(Long userId) {
        return findActiveDog(userId);
    }

    /** 활성 강아지 조회 공통 */
    private UserDog findActiveDog(Long userId) {
        Long activeDogId = findUser(userId).getActiveDogId();
        if (activeDogId == null) {
            throw new BusinessException(ErrorCode.DOG_009);
        }
        return userDogRepository.findByIdAndUserId(activeDogId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOG_009));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
    }

    /** 이름 검증 - 1~7자, 한글/영문/숫자, 비속어 필터(전역 ProfanityFilter 재사용). 중복 허용 */
    private void validateName(String name) {
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException(ErrorCode.DOG_001);
        }
        if (profanityFilter.containsProfanity(name)) {
            throw new BusinessException(ErrorCode.DOG_002);
        }
    }
}

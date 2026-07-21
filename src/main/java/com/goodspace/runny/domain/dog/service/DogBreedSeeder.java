package com.goodspace.runny.domain.dog.service;

import com.goodspace.runny.domain.dog.entity.BreedGrade;
import com.goodspace.runny.domain.dog.entity.DogBreed;
import com.goodspace.runny.domain.dog.repository.DogBreedRepository;
import com.goodspace.runny.global.util.StaticAssetUrls;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 견종 마스터 시드 데이터 등록기. 최초 기동 시 비어 있으면 12종(일반 8 + 레어 4)을 등록한다.
 * 외형은 3D 모델(glb)이며, 저장 후 id 기반 컨벤션(breed/{id}.glb)으로 model_url을 할당한다.
 * 모델러는 해당 파일명으로 S3 breed/ 프리픽스에 업로드만 하면 된다(DB 등록 불필요).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DogBreedSeeder implements CommandLineRunner {

    // 업적 해금형 견종의 업적 코드 (8단계 achievement.code와 매칭)
    public static final String ACHIEVEMENT_BORDER_COLLIE = "BORDER_COLLIE";
    public static final String ACHIEVEMENT_GREYHOUND = "GREYHOUND";

    private static final int NORMAL_PRICE = 1_000;
    private static final int PREMIUM_PRICE = 5_000;

    private final DogBreedRepository dogBreedRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (dogBreedRepository.count() > 0) {
            return;
        }
        List<DogBreed> breeds = dogBreedRepository.saveAll(List.of(
                // 일반 8종 - 온보딩 무료 선택 1종 / 이후 입양 1,000코인 (MVP 전부 동일가)
                normal("치와와", "작지만 용감한 세상에서 제일 작은 견종. 작은 몸에 큰 심장을 담고 어디든 함께 달립니다."),
                normal("시바견", "고집은 세지만 그만큼 매력적인 일본의 국민견. 도도한 표정으로 러닝을 리드합니다."),
                normal("웰시코기", "짧은 다리로 열심히 달리는 모습이 사랑스러운 목축견. 엉덩이 씰룩임이 러닝의 활력소입니다."),
                normal("시츄", "느긋하고 다정한 성격의 반려견. 천천히 오래 달리는 산책형 러닝 메이트입니다."),
                normal("닥스훈트", "긴 몸과 짧은 다리의 소시지 강아지. 낮은 자세로 의외의 스피드를 보여줍니다."),
                normal("퍼그", "찡그린 얼굴이 매력인 개구쟁이. 헥헥거리면서도 끝까지 함께 뛰는 의리파입니다."),
                normal("시고르자브종", "시골 잡종견의 정겨운 이름. 잡초처럼 강한 체력으로 어떤 코스든 거뜬합니다."),
                normal("비숑프리제", "구름 같은 하얀 털의 애교쟁이. 폭신한 발걸음으로 경쾌하게 달립니다."),
                // 레어 - 고가 구매 2종 (5,000코인)
                premium("골든리트리버", "온화하고 똑똑한 만능 반려견. 장거리 러닝도 미소를 잃지 않는 최고의 파트너입니다."),
                premium("도베르만", "늠름한 체격과 절제된 카리스마. 훈련된 주력으로 페이스메이커 역할을 해냅니다."),
                // 레어 - 업적 해금 2종 (해금 후 무료 입양)
                achievementLocked("보더콜리", "세상에서 가장 영리한 견종. 지치지 않는 지구력의 소유자로, 업적 달성 시 함께할 수 있습니다.",
                        ACHIEVEMENT_BORDER_COLLIE),
                achievementLocked("그레이하운드", "시속 70km의 지상 최속 스프린터. 스피드 업적을 달성한 러너에게만 곁을 허락합니다.",
                        ACHIEVEMENT_GREYHOUND)
        ));
        // id 확정 후 모델 URL 할당 (breed/{id}.glb 컨벤션)
        breeds.forEach(breed -> breed.assignModelUrl(StaticAssetUrls.breedModel(breed.getId())));
        log.info("견종 마스터 시드 {}종 등록 완료 (glb 모델 URL 자동 할당)", breeds.size());
    }

    /** 일반 견종 생성 헬퍼 */
    private DogBreed normal(String name, String description) {
        return DogBreed.builder()
                .name(name).description(description)
                .grade(BreedGrade.NORMAL).price(NORMAL_PRICE)
                .build();
    }

    /** 고가 구매형 레어 견종 생성 헬퍼 */
    private DogBreed premium(String name, String description) {
        return DogBreed.builder()
                .name(name).description(description)
                .grade(BreedGrade.RARE).price(PREMIUM_PRICE)
                .build();
    }

    /** 업적 해금형 레어 견종 생성 헬퍼 - 해금 후 무료(price 0) */
    private DogBreed achievementLocked(String name, String description, String achievementCode) {
        return DogBreed.builder()
                .name(name).description(description)
                .grade(BreedGrade.RARE).price(0)
                .unlockAchievementCode(achievementCode)
                .build();
    }
}

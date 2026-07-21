package com.goodspace.runny.domain.item.service;

import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.entity.ItemTier;
import com.goodspace.runny.domain.item.repository.ItemRepository;
import com.goodspace.runny.global.util.StaticAssetUrls;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 아이템 마스터 시드 데이터 등록기. 기획 확정 가격표(2026-07)를 그대로 등록한다.
 * 가격은 등급 단일가가 아닌 분포형: 저렴 ~199 / 중간 200~499 / 고급 500 이상 범위 내 개별 가격.
 * 특수효과는 문서 원문 "(5)" 표기이나 나열 항목 4개, 추후 1종 추가 예정이라 4개만 등록. 총 71종.
 * 외형은 3D 모델(glb)이며, 저장 후 id 기반 컨벤션(item/{id}.glb)으로 model_url을 할당한다.
 * 모델러는 해당 파일명으로 S3 item/ 프리픽스에 업로드만 하면 된다(DB 등록 불필요).
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ItemSeeder implements CommandLineRunner {

    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (itemRepository.count() > 0) {
            return;
        }
        List<Item> items = new ArrayList<>();

        // 목줄 (15) - 저렴 5 / 중간 7 / 고급 3
        cheap(items, ItemCategory.COLLAR, "기본 가죽 목줄", 60);
        cheap(items, ItemCategory.COLLAR, "방울 목줄", 60);
        cheap(items, ItemCategory.COLLAR, "체크무늬 목줄", 100);
        cheap(items, ItemCategory.COLLAR, "리본 목줄", 150);
        cheap(items, ItemCategory.COLLAR, "데님 목줄", 180);
        mid(items, ItemCategory.COLLAR, "진주 목걸이", 200);
        mid(items, ItemCategory.COLLAR, "방울꽃 목걸이", 250);
        mid(items, ItemCategory.COLLAR, "하트 펜던트 목줄", 300);
        mid(items, ItemCategory.COLLAR, "야광 목줄", 300);
        mid(items, ItemCategory.COLLAR, "스포츠 하네스", 350);
        mid(items, ItemCategory.COLLAR, "금색 체인 목걸이", 400);
        mid(items, ItemCategory.COLLAR, "가시 초커(펑크 스타일)", 400);
        premium(items, ItemCategory.COLLAR, "네온 러닝 목줄", 550);
        premium(items, ItemCategory.COLLAR, "반사판 야간러닝 하네스", 700);
        premium(items, ItemCategory.COLLAR, "마라톤 완주 메달 목줄", 1000);

        // 의류 (12) - 저렴 5 / 중간 7 / 고급 0 (망토류 3종 제외 결정)
        cheap(items, ItemCategory.CLOTHES, "체크 반다나", 60);
        cheap(items, ItemCategory.CLOTHES, "땡땡이 스카프", 80);
        cheap(items, ItemCategory.CLOTHES, "태극 문양 스카프", 100);
        cheap(items, ItemCategory.CLOTHES, "크리스마스 니트 스웨터", 150);
        cheap(items, ItemCategory.CLOTHES, "줄무늬 티셔츠", 150);
        mid(items, ItemCategory.CLOTHES, "우비(레인코트)", 200);
        mid(items, ItemCategory.CLOTHES, "후드 집업", 250);
        mid(items, ItemCategory.CLOTHES, "패딩 조끼", 250);
        mid(items, ItemCategory.CLOTHES, "한복 배자", 300);
        mid(items, ItemCategory.CLOTHES, "턱시도 조끼", 300);
        mid(items, ItemCategory.CLOTHES, "등번호 러닝 조끼", 350);
        mid(items, ItemCategory.CLOTHES, "팀 유니폼 조끼", 450);
        // 모자 (15) - 저렴 4 / 중간 6 / 고급 5
        cheap(items, ItemCategory.HAT, "야구모자", 60);
        cheap(items, ItemCategory.HAT, "버킷햇", 60);
        cheap(items, ItemCategory.HAT, "헤드밴드(리본형)", 100);
        cheap(items, ItemCategory.HAT, "헤드밴드(꽃형)", 100);
        mid(items, ItemCategory.HAT, "밀짚모자", 200);
        mid(items, ItemCategory.HAT, "산타모자", 250);
        mid(items, ItemCategory.HAT, "헤일로(천사 링)", 250);
        mid(items, ItemCategory.HAT, "헬멧(스포츠형)", 300);
        mid(items, ItemCategory.HAT, "고글", 400);
        mid(items, ItemCategory.HAT, "선글라스", 400);
        premium(items, ItemCategory.HAT, "악마 뿔 머리띠", 500);
        premium(items, ItemCategory.HAT, "토끼귀 머리띠", 500);
        premium(items, ItemCategory.HAT, "삐약이 병아리 모자", 500);
        premium(items, ItemCategory.HAT, "졸업모", 500);
        premium(items, ItemCategory.HAT, "왕관", 700);

        // 신발 (10) - 저렴 2 / 중간 5 / 고급 3
        cheap(items, ItemCategory.SHOES, "슬리퍼", 60);
        cheap(items, ItemCategory.SHOES, "겨울 방한화", 100);
        mid(items, ItemCategory.SHOES, "레인부츠", 200);
        mid(items, ItemCategory.SHOES, "줄무늬 양말", 200);
        mid(items, ItemCategory.SHOES, "크리스마스 양말", 250);
        mid(items, ItemCategory.SHOES, "발레 슈즈", 300);
        mid(items, ItemCategory.SHOES, "축구화", 400);
        premium(items, ItemCategory.SHOES, "눈꽃 양말", 500);
        premium(items, ItemCategory.SHOES, "러닝화 4족 세트", 700);
        premium(items, ItemCategory.SHOES, "LED 라이트업 신발", 1000);

        // 장난감 (15) - 저렴 5 / 중간 6 / 고급 4
        cheap(items, ItemCategory.TOY, "낡은 슬리퍼 장난감", 60);
        cheap(items, ItemCategory.TOY, "공(테니스볼)", 60);
        cheap(items, ItemCategory.TOY, "축구공", 80);
        cheap(items, ItemCategory.TOY, "원반(프리스비)", 80);
        cheap(items, ItemCategory.TOY, "삑삑이 인형", 100);
        mid(items, ItemCategory.TOY, "로프토이", 200);
        mid(items, ItemCategory.TOY, "뼈다귀 장난감", 250);
        mid(items, ItemCategory.TOY, "인형(곰돌이)", 250);
        mid(items, ItemCategory.TOY, "러닝 파트너 볼(발광볼)", 300);
        mid(items, ItemCategory.TOY, "병아리 인형", 300);
        mid(items, ItemCategory.TOY, "도넛 인형", 300);
        premium(items, ItemCategory.TOY, "터그토이", 550);
        premium(items, ItemCategory.TOY, "퍼즐토이", 550);
        premium(items, ItemCategory.TOY, "로켓 모양 장난감", 700);
        premium(items, ItemCategory.TOY, "미니 원반(스피드 훈련용)", 700);

        // 특수효과 (4) - 전부 고급. 문서 원문 "(5)" 표기이나 나열 항목 4개, 추후 1종 추가 예정
        premium(items, ItemCategory.EFFECT, "반짝이는 별 이펙트", 1000);
        premium(items, ItemCategory.EFFECT, "무지개 트레일(달릴 때 궤적)", 1200);
        premium(items, ItemCategory.EFFECT, "불꽃 발자국 이펙트", 1200);
        premium(items, ItemCategory.EFFECT, "하트 뿅뿅 이펙트", 1500);

        List<Item> saved = itemRepository.saveAll(items);
        // id 확정 후 모델 URL 할당 (item/{id}.glb 컨벤션)
        saved.forEach(item -> item.assignModelUrl(StaticAssetUrls.itemModel(item.getId())));
        log.info("아이템 마스터 시드 {}종 등록 완료 (glb 모델 URL 자동 할당)", saved.size());
    }

    /** 저렴 등급 (~199) 아이템 추가 헬퍼 */
    private void cheap(List<Item> items, ItemCategory category, String name, int price) {
        items.add(new Item(category, name, ItemTier.CHEAP, price));
    }

    /** 중간 등급 (200~499) 아이템 추가 헬퍼 */
    private void mid(List<Item> items, ItemCategory category, String name, int price) {
        items.add(new Item(category, name, ItemTier.MID, price));
    }

    /** 고급 등급 (500 이상) 아이템 추가 헬퍼 */
    private void premium(List<Item> items, ItemCategory category, String name, int price) {
        items.add(new Item(category, name, ItemTier.PREMIUM, price));
    }
}

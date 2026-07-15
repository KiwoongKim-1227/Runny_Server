package com.goodspace.runny.domain.item.service;

import com.goodspace.runny.domain.item.entity.Item;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.entity.ItemTier;
import com.goodspace.runny.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 아이템 마스터 시드 데이터 등록기. 기획 문서 4.E 시드 표를 그대로 등록한다.
 * 가격은 등급 단일가가 아닌 분포형: 저렴 60~199 / 중간 200~999 / 고급 1000~2000 범위 내 개별 가격.
 * 특수효과는 문서 주석대로 4종만 등록(추후 1종 추가 예정), 총 74종.
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
        cheap(items, ItemCategory.COLLAR, "방울 목줄", 80);
        cheap(items, ItemCategory.COLLAR, "체크무늬 목줄", 100);
        cheap(items, ItemCategory.COLLAR, "리본 목줄", 110);
        cheap(items, ItemCategory.COLLAR, "데님 목줄", 120);
        mid(items, ItemCategory.COLLAR, "방울꽃 목걸이", 250);
        mid(items, ItemCategory.COLLAR, "진주 목걸이", 350);
        mid(items, ItemCategory.COLLAR, "하트 펜던트 목줄", 420);
        mid(items, ItemCategory.COLLAR, "야광 목줄", 480);
        mid(items, ItemCategory.COLLAR, "가시 초커(펑크 스타일)", 550);
        mid(items, ItemCategory.COLLAR, "스포츠 하네스", 700);
        mid(items, ItemCategory.COLLAR, "금색 체인 목걸이", 900);
        premium(items, ItemCategory.COLLAR, "네온 러닝 목줄", 1100);
        premium(items, ItemCategory.COLLAR, "반사판 야간러닝 하네스", 1400);
        premium(items, ItemCategory.COLLAR, "마라톤 완주 메달 목줄", 1900);

        // 의류 (15) - 저렴 5 / 중간 7 / 고급 3
        cheap(items, ItemCategory.CLOTHES, "체크 반다나", 70);
        cheap(items, ItemCategory.CLOTHES, "땡땡이 스카프", 90);
        cheap(items, ItemCategory.CLOTHES, "태극 문양 스카프", 130);
        cheap(items, ItemCategory.CLOTHES, "줄무늬 티셔츠", 150);
        cheap(items, ItemCategory.CLOTHES, "크리스마스 니트 스웨터", 190);
        mid(items, ItemCategory.CLOTHES, "우비(레인코트)", 280);
        mid(items, ItemCategory.CLOTHES, "후드 집업", 380);
        mid(items, ItemCategory.CLOTHES, "등번호 러닝 조끼", 450);
        mid(items, ItemCategory.CLOTHES, "패딩 조끼", 560);
        mid(items, ItemCategory.CLOTHES, "팀 유니폼 조끼", 620);
        mid(items, ItemCategory.CLOTHES, "한복 배자", 780);
        mid(items, ItemCategory.CLOTHES, "턱시도 조끼", 850);
        premium(items, ItemCategory.CLOTHES, "슈퍼히어로 망토", 1200);
        premium(items, ItemCategory.CLOTHES, "유니콘 망토", 1500);
        premium(items, ItemCategory.CLOTHES, "왕관 케이프", 2000);

        // 모자 (15) - 저렴 4 / 중간 6 / 고급 5
        cheap(items, ItemCategory.HAT, "야구모자", 80);
        cheap(items, ItemCategory.HAT, "버킷햇", 120);
        cheap(items, ItemCategory.HAT, "헤드밴드(리본형)", 140);
        cheap(items, ItemCategory.HAT, "헤드밴드(꽃형)", 160);
        mid(items, ItemCategory.HAT, "밀짚모자", 260);
        mid(items, ItemCategory.HAT, "산타모자", 330);
        mid(items, ItemCategory.HAT, "헬멧(스포츠형)", 470);
        mid(items, ItemCategory.HAT, "고글", 540);
        mid(items, ItemCategory.HAT, "선글라스", 680);
        mid(items, ItemCategory.HAT, "헤일로(천사 링)", 820);
        premium(items, ItemCategory.HAT, "악마 뿔 머리띠", 1050);
        premium(items, ItemCategory.HAT, "토끼귀 머리띠", 1250);
        premium(items, ItemCategory.HAT, "삐약이 병아리 모자", 1450);
        premium(items, ItemCategory.HAT, "졸업모", 1650);
        premium(items, ItemCategory.HAT, "왕관", 2000);

        // 신발 (10) - 저렴 2 / 중간 5 / 고급 3
        cheap(items, ItemCategory.SHOES, "슬리퍼", 90);
        cheap(items, ItemCategory.SHOES, "겨울 방한화", 170);
        mid(items, ItemCategory.SHOES, "줄무늬 양말", 240);
        mid(items, ItemCategory.SHOES, "크리스마스 양말", 320);
        mid(items, ItemCategory.SHOES, "레인부츠", 430);
        mid(items, ItemCategory.SHOES, "발레 슈즈", 590);
        mid(items, ItemCategory.SHOES, "축구화", 760);
        premium(items, ItemCategory.SHOES, "눈꽃 양말", 1100);
        premium(items, ItemCategory.SHOES, "LED 라이트업 신발", 1500);
        premium(items, ItemCategory.SHOES, "러닝화 4족 세트", 1800);

        // 장난감 (15) - 저렴 5 / 중간 6 / 고급 4
        cheap(items, ItemCategory.TOY, "낡은 슬리퍼 장난감", 60);
        cheap(items, ItemCategory.TOY, "공(테니스볼)", 100);
        cheap(items, ItemCategory.TOY, "축구공", 130);
        cheap(items, ItemCategory.TOY, "원반(프리스비)", 150);
        cheap(items, ItemCategory.TOY, "삑삑이 인형", 180);
        mid(items, ItemCategory.TOY, "로프토이", 230);
        mid(items, ItemCategory.TOY, "뼈다귀 장난감", 310);
        mid(items, ItemCategory.TOY, "인형(곰돌이)", 400);
        mid(items, ItemCategory.TOY, "병아리 인형", 520);
        mid(items, ItemCategory.TOY, "도넛 인형", 640);
        mid(items, ItemCategory.TOY, "러닝 파트너 볼(발광볼)", 830);
        premium(items, ItemCategory.TOY, "터그토이", 1150);
        premium(items, ItemCategory.TOY, "퍼즐토이", 1350);
        premium(items, ItemCategory.TOY, "미니 원반(스피드 훈련용)", 1550);
        premium(items, ItemCategory.TOY, "로켓 모양 장난감", 1750);

        // 특수효과 (4) - 전부 고급. 문서 원문 "(5)" 표기이나 나열 항목 4개, 추후 1종 추가 예정
        premium(items, ItemCategory.EFFECT, "반짝이는 별 이펙트", 1300);
        premium(items, ItemCategory.EFFECT, "하트 뿅뿅 이펙트", 1500);
        premium(items, ItemCategory.EFFECT, "불꽃 발자국 이펙트", 1800);
        premium(items, ItemCategory.EFFECT, "무지개 트레일(달릴 때 궤적)", 2000);

        itemRepository.saveAll(items);
        log.info("아이템 마스터 시드 {}종 등록 완료", items.size());
    }

    /** 저렴 등급 (60~199) 아이템 추가 헬퍼 */
    private void cheap(List<Item> items, ItemCategory category, String name, int price) {
        items.add(new Item(category, name, ItemTier.CHEAP, price, imageUrl(category, name)));
    }

    /** 중간 등급 (200~999) 아이템 추가 헬퍼 */
    private void mid(List<Item> items, ItemCategory category, String name, int price) {
        items.add(new Item(category, name, ItemTier.MID, price, imageUrl(category, name)));
    }

    /** 고급 등급 (1000~2000) 아이템 추가 헬퍼 */
    private void premium(List<Item> items, ItemCategory category, String name, int price) {
        items.add(new Item(category, name, ItemTier.PREMIUM, price, imageUrl(category, name)));
    }

    /** S3 정적 리소스 경로 규칙 (운영자 업로드, 8.4 item/ 프리픽스) */
    private String imageUrl(ItemCategory category, String name) {
        return "https://runny-assets.s3.ap-northeast-2.amazonaws.com/item/"
                + category.name().toLowerCase() + "/" + name + ".png";
    }
}

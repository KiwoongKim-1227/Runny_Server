package com.goodspace.runny.domain.running.service;

import com.goodspace.runny.domain.running.entity.Sticker;
import com.goodspace.runny.domain.running.repository.StickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 히스토리 꾸미기 스티커 시드 등록기. 이미지는 운영자가 S3 sticker/ 프리픽스에 업로드하는 정적 리소스.
 */
@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
public class StickerSeeder implements CommandLineRunner {

    private static final String BASE_URL = "https://runny-assets.s3.ap-northeast-2.amazonaws.com/sticker/";

    private final StickerRepository stickerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (stickerRepository.count() > 0) {
            return;
        }
        stickerRepository.saveAll(List.of(
                new Sticker("발자국", BASE_URL + "pawprint.png"),
                new Sticker("뼈다귀", BASE_URL + "bone.png"),
                new Sticker("하트", BASE_URL + "heart.png"),
                new Sticker("왕관", BASE_URL + "crown.png"),
                new Sticker("번개", BASE_URL + "lightning.png"),
                new Sticker("메달", BASE_URL + "medal.png"),
                new Sticker("말풍선 멍!", BASE_URL + "speech_woof.png"),
                new Sticker("반짝이 별", BASE_URL + "sparkle_star.png")
        ));
        log.info("스티커 시드 8종 등록 완료");
    }
}

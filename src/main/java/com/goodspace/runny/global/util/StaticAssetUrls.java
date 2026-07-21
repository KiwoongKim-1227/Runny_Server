package com.goodspace.runny.global.util;

/**
 * 운영자 등록 정적 리소스(S3) URL 규칙 유틸.
 * 강아지/아이템 외형은 3D 모델(glb), 업적 아이콘/스티커 등은 이미지(png)를 사용한다.
 * 모델 파일은 id 기반 파일명 컨벤션(breed/{id}.glb, item/{id}.glb)을 따르므로,
 * 모델러가 해당 키로 S3에 업로드만 하면 DB 등록 작업 없이 즉시 반영된다.
 */
public final class StaticAssetUrls {

    // 정적 리소스 버킷 base URL (버킷 변경 시 이 값만 수정)
    public static final String BASE = "https://runny-assets.s3.ap-northeast-2.amazonaws.com/";

    private StaticAssetUrls() {
    }

    /** 견종 3D 모델 URL - breed/{breedId}.glb */
    public static String breedModel(Long breedId) {
        return BASE + "breed/" + breedId + ".glb";
    }

    /** 아이템(코스튬) 3D 모델 URL - item/{itemId}.glb */
    public static String itemModel(Long itemId) {
        return BASE + "item/" + itemId + ".glb";
    }
}

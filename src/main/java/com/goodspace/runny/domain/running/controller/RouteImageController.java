package com.goodspace.runny.domain.running.controller;

import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import com.goodspace.runny.global.util.S3Uploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 러닝 경로 이미지 업로드 컨트롤러. 프론트가 GPS 좌표로 생성한 2종(지도 캡처/투명 경로 선)을
 * 각각 업로드한다. S3 key는 route/{userId}/ 프리픽스로 저장해 회원 탈퇴 시 일괄 삭제가 가능하다.
 */
@Tag(name = "RouteImage", description = "러닝 경로 이미지 업로드 - 캡처/경로 선 2종, S3 route/ 프리픽스")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class RouteImageController {

    private final S3Uploader s3Uploader;

    /** 경로 이미지 업로드 - 업로드 후 URL을 러닝 종료(complete) 요청에 전달한다 */
    @Operation(summary = "경로 이미지 업로드",
            description = "지도 캡처(route_image)와 투명 배경 경로 선(route_line_image)을 프론트가 각각 업로드. "
                    + "jpg/jpeg/png/webp, 최대 10MB(IMAGE_001/IMAGE_002). 좌표 원본은 서버에 저장하지 않는다")
    @PostMapping(value = "/route", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadRouteImage(@RequestPart MultipartFile image) {
        // 유저별 프리픽스로 저장 - 탈퇴 시 route/{userId}/ 일괄 삭제 대상 (문서 8.4)
        String url = s3Uploader.upload(image, "route/" + SecurityUtil.currentUserId() + "/");
        return ApiResponse.ok(Map.of("imageUrl", url));
    }
}

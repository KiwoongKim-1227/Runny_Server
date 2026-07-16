package com.goodspace.runny.domain.achievement.controller;

import com.goodspace.runny.domain.achievement.dto.AchievementDto;
import com.goodspace.runny.domain.achievement.service.AchievementService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 업적 API 컨트롤러. 목록 조회와 보상 수령을 제공한다. 달성 판정은 러닝 완료/친구 수락 이벤트가 수행.
 */
@Tag(name = "Achievement", description = "업적 API - 목록(달성률), 수령 버튼 방식 보상(코인/견종 해금)")
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /** 업적 목록 조회 */
    @Operation(summary = "업적 목록 조회",
            description = "아이콘(imageUrl)/달성(achieved)/수령(claimed) 여부 + 달성률. 프론트 필터(전체·달성·미달성)는 achieved로 분기")
    @GetMapping
    public ApiResponse<AchievementDto.ListResponse> getAchievements() {
        return ApiResponse.ok(achievementService.getAchievements(SecurityUtil.currentUserId()));
    }

    /** 업적 보상 수령 */
    @Operation(summary = "업적 보상 수령",
            description = "달성 상태에서만 가능. 코인형은 코인 지급, 견종 해금형은 수령 시점이 해금(이후 무료 입양 가능). "
                    + "미달성 ACHIEVEMENT_001, 중복 수령 ACHIEVEMENT_002")
    @PostMapping("/{achievementId}/claim")
    public ApiResponse<AchievementDto.ClaimResponse> claim(@PathVariable Long achievementId) {
        return ApiResponse.ok(achievementService.claim(SecurityUtil.currentUserId(), achievementId));
    }
}

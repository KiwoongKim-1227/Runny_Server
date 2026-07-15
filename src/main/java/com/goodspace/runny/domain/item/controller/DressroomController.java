package com.goodspace.runny.domain.item.controller;

import com.goodspace.runny.domain.item.dto.DressroomDto;
import com.goodspace.runny.domain.item.entity.ItemCategory;
import com.goodspace.runny.domain.item.service.DressroomService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 드레스룸 API 컨트롤러. 보유 아이템 조회, 활성 강아지 코디 저장/조회를 제공한다.
 */
@Tag(name = "Dressroom", description = "드레스룸 API - 보유 아이템 조회, 코디 저장(슬롯 6종)")
@RestController
@RequestMapping("/api/dressroom")
@RequiredArgsConstructor
public class DressroomController {

    private final DressroomService dressroomService;

    /** 카테고리별 보유 아이템 조회 */
    @Operation(summary = "카테고리별 보유 아이템 조회",
            description = "보유 아이템 목록 + 현재 착용 아이템 ID(equippedItemId, 미착용 null). "
                    + "보유가 없으면 items는 빈 배열. category: COLLAR/CLOTHES/HAT/SHOES/TOY/EFFECT")
    @GetMapping("/items")
    public ApiResponse<DressroomDto.CategoryItems> getOwnedItems(@RequestParam ItemCategory category) {
        return ApiResponse.ok(dressroomService.getOwnedItems(SecurityUtil.currentUserId(), category));
    }

    /** 활성 강아지 코디 저장 (6개 슬롯 전체 교체) */
    @Operation(summary = "코디 저장 (전체 교체)",
            description = "6개 슬롯의 itemId를 한 번에 저장하며 null은 해당 슬롯 해제. "
                    + "미보유 아이템 ITEM_002, 슬롯-카테고리 불일치 ITEM_003, 존재하지 않는 아이템 ITEM_001. "
                    + "착용 상태는 강아지별 저장이므로 활성 강아지에게만 반영된다")
    @PutMapping("/equipment")
    public ApiResponse<DressroomDto.EquipmentResponse> saveEquipment(
            @Valid @RequestBody DressroomDto.EquipmentSaveRequest request) {
        return ApiResponse.ok(dressroomService.saveEquipment(SecurityUtil.currentUserId(), request));
    }

    /** 저장된 코디 조회 (되돌리기/초기화 용) */
    @Operation(summary = "저장된 코디 조회",
            description = "드레스룸 되돌리기/상점 초기화 버튼은 이 API 재조회로 처리한다. 슬롯별 itemId(미착용 null) 반환")
    @GetMapping("/equipment")
    public ApiResponse<DressroomDto.EquipmentResponse> getEquipment() {
        return ApiResponse.ok(dressroomService.getEquipment(SecurityUtil.currentUserId()));
    }
}

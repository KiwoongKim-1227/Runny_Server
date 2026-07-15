package com.goodspace.runny.domain.friend.controller;

import com.goodspace.runny.domain.friend.dto.FriendDto;
import com.goodspace.runny.domain.friend.service.PlaygroundService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 놀이터 API 컨트롤러. 메인 조회와 초대 목록 저장을 제공한다.
 */
@Tag(name = "Playground", description = "놀이터(메인) API - 내/초대 강아지 조회, 초대 관리")
@RestController
@RequestMapping("/api/playground")
@RequiredArgsConstructor
public class PlaygroundController {

    private final PlaygroundService playgroundService;

    /** 놀이터 메인 조회 */
    @Operation(summary = "놀이터 메인 조회",
            description = "내 활성 강아지(착용 아이템 외형 포함) + 초대한 친구들의 활성 강아지 + 보유 코인 + "
                    + "hasDogProfileBadge(펫 프로필 미확인 변경 빨간 점). 온보딩 미완료면 활성 강아지가 없어 DOG 관련 정보가 비어있을 수 있다")
    @GetMapping
    public ApiResponse<FriendDto.PlaygroundResponse> getPlayground() {
        return ApiResponse.ok(playgroundService.getPlayground(SecurityUtil.currentUserId()));
    }

    /** 놀이터 초대 목록 저장 */
    @Operation(summary = "놀이터 초대 목록 저장 (전체 교체)",
            description = "친구 ID 배열(최대 4명, 초과 시 FRIEND_005) 전체 교체(PUT). 친구가 아닌 유저 포함 시 FRIEND_004. "
                    + "초대는 나에게만 보이며 상대에게 알림/노출되지 않는다")
    @PutMapping("/invites")
    public ApiResponse<Void> saveInvites(@Valid @RequestBody FriendDto.InviteSaveRequest request) {
        playgroundService.saveInvites(SecurityUtil.currentUserId(), request);
        return ApiResponse.ok();
    }
}

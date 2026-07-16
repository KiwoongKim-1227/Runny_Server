package com.goodspace.runny.domain.friend.controller;

import com.goodspace.runny.domain.friend.dto.FriendDto;
import com.goodspace.runny.domain.friend.service.FriendService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 친구 API 컨트롤러. 목록/검색/상세, 요청 보내기/취소/수락/거절, 친구 삭제를 제공한다.
 */
@Tag(name = "Friend", description = "친구 API - 검색/요청/수락/삭제, UserSummary 기반 응답")
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /** 친구 목록 */
    @Operation(summary = "친구 목록",
            description = "UserSummary(닉네임/강아지 이름/견종/레벨/외형) 기반. "
                    + "놀이터 초대된 친구가 최상단 + isPlayingTogether 플래그")
    @GetMapping
    public ApiResponse<List<FriendDto.FriendItem>> getFriends() {
        return ApiResponse.ok(friendService.getFriends(SecurityUtil.currentUserId()));
    }

    /** 친구 상세 */
    @Operation(summary = "친구 상세", description = "친구 강아지 프로필 팝업용 - UserSummary + 강아지 스탯. 친구가 아니면 FRIEND_004")
    @GetMapping("/{friendUserId}/detail")
    public ApiResponse<FriendDto.FriendDetail> getFriendDetail(@PathVariable Long friendUserId) {
        return ApiResponse.ok(friendService.getFriendDetail(SecurityUtil.currentUserId(), friendUserId));
    }

    /** 친구 검색 */
    @Operation(summary = "친구 검색",
            description = "닉네임 부분 일치, 전체 반환. 각 결과에 관계 상태 포함 - "
                    + "NONE(+ 버튼)/REQUESTED(내가 요청함)/RECEIVED(상대가 요청함)/FRIEND(이미 친구)")
    @GetMapping("/search")
    public ApiResponse<FriendDto.SearchResponse> search(@RequestParam String nickname) {
        return ApiResponse.ok(friendService.search(SecurityUtil.currentUserId(), nickname));
    }

    /** 친구 요청 보내기 */
    @Operation(summary = "친구 요청 보내기",
            description = "자기 자신 FRIEND_001, 중복 요청 FRIEND_002(동시 요청도 DB UNIQUE로 차단), 이미 친구 FRIEND_003")
    @PostMapping("/requests")
    public ApiResponse<Void> sendRequest(@Valid @RequestBody FriendDto.RequestSend request) {
        friendService.sendRequest(SecurityUtil.currentUserId(), request.targetUserId());
        return ApiResponse.ok();
    }

    /** 보낸 요청 취소 */
    @Operation(summary = "보낸 요청 취소", description = "요청자 본인 + PENDING만 가능. 이미 처리된 요청 FRIEND_007")
    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> cancelRequest(@PathVariable Long requestId) {
        friendService.cancelRequest(SecurityUtil.currentUserId(), requestId);
        return ApiResponse.ok();
    }

    /** 보낸 요청 목록 */
    @Operation(summary = "보낸 요청 목록", description = "PENDING 상태만 반환")
    @GetMapping("/requests/sent")
    public ApiResponse<List<FriendDto.RequestItem>> getSentRequests() {
        return ApiResponse.ok(friendService.getSentRequests(SecurityUtil.currentUserId()));
    }

    /** 받은 요청 목록 */
    @Operation(summary = "받은 요청 목록",
            description = "PENDING만 반환. hadUnchecked로 빨간 점 여부를 알려주고 조회 시점에 일괄 확인 처리된다")
    @GetMapping("/requests/received")
    public ApiResponse<FriendDto.ReceivedRequests> getReceivedRequests() {
        return ApiResponse.ok(friendService.getReceivedRequests(SecurityUtil.currentUserId()));
    }

    /** 요청 수락 */
    @Operation(summary = "친구 요청 수락", description = "수신자 본인만 가능. 수락 시 친구 사귀기 업적 판정 훅 호출(8단계 연결)")
    @PostMapping("/requests/{requestId}/accept")
    public ApiResponse<Void> acceptRequest(@PathVariable Long requestId) {
        friendService.acceptRequest(SecurityUtil.currentUserId(), requestId);
        return ApiResponse.ok();
    }

    /** 요청 거절 */
    @Operation(summary = "친구 요청 거절", description = "수신자 본인만 가능. 거절은 삭제로 처리되어 상대가 재요청할 수 있다")
    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> rejectRequest(@PathVariable Long requestId) {
        friendService.rejectRequest(SecurityUtil.currentUserId(), requestId);
        return ApiResponse.ok();
    }

    /** 친구 삭제 */
    @Operation(summary = "친구 삭제", description = "관계 삭제 시 서로의 놀이터 초대도 함께 제거된다")
    @DeleteMapping("/{friendUserId}")
    public ApiResponse<Void> deleteFriend(@PathVariable Long friendUserId) {
        friendService.deleteFriend(SecurityUtil.currentUserId(), friendUserId);
        return ApiResponse.ok();
    }
}

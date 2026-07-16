package com.goodspace.runny.domain.crew.controller;

import com.goodspace.runny.domain.crew.dto.CrewDto;
import com.goodspace.runny.domain.crew.service.CrewAdminService;
import com.goodspace.runny.domain.crew.service.CrewService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 크루 API 컨트롤러. 검색/상세/생성/가입 신청과 크루장 관리 기능을 제공한다.
 */
@Tag(name = "Crew", description = "크루 API - 검색/상세/생성/가입, 크루장 관리(승인/추방/해체/변경/위임)")
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;
    private final CrewAdminService crewAdminService;

    /** 크루 검색 */
    @Operation(summary = "크루 검색",
            description = "크루명 부분 일치, 전체 반환. memberCount와 myRequestStatus(NONE/PENDING)로 "
                    + "가입 신청 버튼 vs 승인대기 표시 분기")
    @GetMapping("/search")
    public ApiResponse<CrewDto.SearchResponse> search(@RequestParam String name) {
        return ApiResponse.ok(crewService.search(SecurityUtil.currentUserId(), name));
    }

    /** 크루명 중복확인 */
    @Operation(summary = "크루명 중복확인", description = "최대 8자(CREW_002) + 비속어(CREW_003) 검증, 중복이면 available=false")
    @GetMapping("/name/check")
    public ApiResponse<CrewDto.NameCheckResponse> checkName(@RequestParam String name) {
        return ApiResponse.ok(new CrewDto.NameCheckResponse(crewService.isNameAvailable(name)));
    }

    /** 크루 상세 */
    @Operation(summary = "크루 상세",
            description = "이미지/크루명/한줄소개/총 누적 거리/멤버 수(현재·최대)/이번 주 top3(월요일 00:00 KST 기준)/"
                    + "크루원 목록(UserSummary로 강아지 외형 포함). 미가입자 검색 팝업과 크루원 메인이 동일 데이터 사용")
    @GetMapping("/{crewId}")
    public ApiResponse<CrewDto.DetailResponse> getDetail(@PathVariable Long crewId) {
        return ApiResponse.ok(crewService.getDetail(crewId));
    }

    /** 크루 생성 (multipart - 로고 이미지 선택) */
    @Operation(summary = "크루 생성",
            description = "로고는 선택(없으면 기본 이미지). 크루명 8자+중복+비속어, 한줄소개 30자 검증. "
                    + "생성자는 LEADER. 이미 크루 소속이면 CREW_005")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Long>> create(
            @RequestParam String name,
            @RequestParam(required = false) String intro,
            @RequestPart(required = false) MultipartFile image) {
        Long crewId = crewService.create(SecurityUtil.currentUserId(), name, intro, image);
        return ApiResponse.ok(Map.of("crewId", crewId));
    }

    /** 내 크루 조회 */
    @Operation(summary = "내 크루 조회",
            description = "role 포함. 크루장이면 pendingRequestCount(대기 중 가입 신청 수 - 관리 버튼 빨간 배지) 포함. 미소속이면 joined=false")
    @GetMapping("/me")
    public ApiResponse<CrewDto.MyCrewResponse> getMyCrew() {
        return ApiResponse.ok(crewService.getMyCrew(SecurityUtil.currentUserId()));
    }

    /** 가입 신청 */
    @Operation(summary = "가입 신청", description = "1인 1크루(CREW_005), 중복 신청(CREW_007), 정원 초과(CREW_008) 검증")
    @PostMapping("/{crewId}/join-requests")
    public ApiResponse<Void> requestJoin(@PathVariable Long crewId) {
        crewService.requestJoin(SecurityUtil.currentUserId(), crewId);
        return ApiResponse.ok();
    }

    /** 가입 신청 취소 */
    @Operation(summary = "가입 신청 취소", description = "본인의 PENDING 신청만 취소 가능")
    @DeleteMapping("/{crewId}/join-requests")
    public ApiResponse<Void> cancelJoin(@PathVariable Long crewId) {
        crewService.cancelJoin(SecurityUtil.currentUserId(), crewId);
        return ApiResponse.ok();
    }

    /** 가입 신청 목록 (크루장) */
    @Operation(summary = "가입 신청 목록 (크루장)",
            description = "PENDING만 반환, 신청자는 UserSummary(강아지 외형 포함). 처리된 요청은 목록에서 즉시 제거. 크루장 아니면 CREW_009")
    @GetMapping("/{crewId}/join-requests")
    public ApiResponse<List<CrewDto.JoinRequestItem>> getJoinRequests(@PathVariable Long crewId) {
        return ApiResponse.ok(crewAdminService.getJoinRequests(crewId, SecurityUtil.currentUserId()));
    }

    /** 가입 신청 일괄 승인 (크루장) */
    @Operation(summary = "가입 신청 일괄 승인 (크루장)",
            description = "요청 ID 배열. 승인 시점 정원 재검증 - 초과분은 failed에 사유와 함께 반환. "
                    + "승인 성공 건은 크루 승인 알림 훅 호출(8단계 연결)")
    @PostMapping("/{crewId}/join-requests/approve")
    public ApiResponse<CrewDto.BatchResult> approve(@PathVariable Long crewId,
                                                    @Valid @RequestBody CrewDto.BatchRequest request) {
        return ApiResponse.ok(crewAdminService.approveAll(crewId, SecurityUtil.currentUserId(), request.requestIds()));
    }

    /** 가입 신청 일괄 거절 (크루장) */
    @Operation(summary = "가입 신청 일괄 거절 (크루장)", description = "요청 ID 배열 일괄 거절")
    @PostMapping("/{crewId}/join-requests/reject")
    public ApiResponse<CrewDto.BatchResult> reject(@PathVariable Long crewId,
                                                   @Valid @RequestBody CrewDto.BatchRequest request) {
        return ApiResponse.ok(crewAdminService.rejectAll(crewId, SecurityUtil.currentUserId(), request.requestIds()));
    }

    /** 크루 탈퇴 */
    @Operation(summary = "크루 탈퇴", description = "일반 크루원만 가능. 크루장은 위임/해체 후 탈퇴(CREW_010)")
    @DeleteMapping("/{crewId}/members/me")
    public ApiResponse<Void> leave(@PathVariable Long crewId) {
        crewService.leave(SecurityUtil.currentUserId(), crewId);
        return ApiResponse.ok();
    }

    /** 크루원 추방 (크루장) */
    @Operation(summary = "크루원 추방 (크루장)", description = "자기 자신 추방 불가(CREW_013), 크루원 아니면 CREW_014")
    @DeleteMapping("/{crewId}/members/{userId}")
    public ApiResponse<Void> kick(@PathVariable Long crewId, @PathVariable Long userId) {
        crewAdminService.kick(crewId, SecurityUtil.currentUserId(), userId);
        return ApiResponse.ok();
    }

    /** 크루 해체 (크루장) */
    @Operation(summary = "크루 해체 (크루장)", description = "멤버십/가입 신청 전부 삭제, 로고 S3 이미지도 삭제(커밋 후 수행)")
    @DeleteMapping("/{crewId}")
    public ApiResponse<Void> disband(@PathVariable Long crewId) {
        crewAdminService.disband(crewId, SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }

    /** 크루명 변경 (크루장, 1,000코인) */
    @Operation(summary = "크루명 변경 (크루장)", description = "1,000코인 차감 + 8자/중복확인/비속어 재검증. 잔액 부족 COIN_001")
    @PatchMapping("/{crewId}/name")
    public ApiResponse<Void> changeName(@PathVariable Long crewId, @RequestParam String name) {
        crewAdminService.changeName(crewId, SecurityUtil.currentUserId(), name);
        return ApiResponse.ok();
    }

    /** 한줄소개 변경 (크루장, 무료) */
    @Operation(summary = "한줄소개 변경 (크루장)", description = "무료, 최대 30자(CREW_004)")
    @PatchMapping("/{crewId}/intro")
    public ApiResponse<Void> changeIntro(@PathVariable Long crewId, @RequestParam String intro) {
        crewAdminService.changeIntro(crewId, SecurityUtil.currentUserId(), intro);
        return ApiResponse.ok();
    }

    /** 이미지 변경 (크루장) */
    @Operation(summary = "크루 이미지 변경 (크루장)", description = "신규 업로드 후 기존 S3 객체는 커밋 후 삭제")
    @PatchMapping(value = "/{crewId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> changeImage(@PathVariable Long crewId,
                                                        @RequestPart MultipartFile image) {
        String imageUrl = crewAdminService.changeImage(crewId, SecurityUtil.currentUserId(), image);
        return ApiResponse.ok(Map.of("imageUrl", imageUrl));
    }

    /** 정원 확장 (크루장, 1,000코인) */
    @Operation(summary = "정원 확장 (크루장)", description = "1,000코인당 +50명. 확장 후 최대 인원 반환")
    @PatchMapping("/{crewId}/capacity")
    public ApiResponse<Map<String, Integer>> expandCapacity(@PathVariable Long crewId) {
        int maxMembers = crewAdminService.expandCapacity(crewId, SecurityUtil.currentUserId());
        return ApiResponse.ok(Map.of("maxMembers", maxMembers));
    }

    /** 크루장 위임 (크루장) */
    @Operation(summary = "크루장 위임 (크루장)", description = "상대 수락 없이 즉시 위임, 본인은 MEMBER로 전환")
    @PatchMapping("/{crewId}/leader")
    public ApiResponse<Void> delegateLeader(@PathVariable Long crewId, @RequestParam Long targetUserId) {
        crewAdminService.delegateLeader(crewId, SecurityUtil.currentUserId(), targetUserId);
        return ApiResponse.ok();
    }
}

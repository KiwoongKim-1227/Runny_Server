package com.goodspace.runny.domain.running.controller;

import com.goodspace.runny.domain.running.dto.RunningDto;
import com.goodspace.runny.domain.running.service.RunningService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 러닝 API 컨트롤러. 세션 시작, 종료 데이터 수신, 리포트, 히스토리, 스티커를 제공한다.
 * 실시간 측정(GPS/일시정지/구간 계산)은 전부 프론트 책임이다.
 */
@Tag(name = "Running", description = "러닝 API - 시작/종료(리포트 생성 + 후속 처리), 히스토리, 꾸미기 데이터")
@RestController
@RequiredArgsConstructor
public class RunningController {

    private final RunningService runningService;

    /** 러닝 세션 시작 (선택적) */
    @Operation(summary = "러닝 세션 시작 (선택적)",
            description = "시작 시각/사용 강아지 기록. 네트워크 오류 시 프론트가 로컬 진행 후 종료 API만으로 기록 생성 가능")
    @PostMapping("/api/runnings/start")
    public ApiResponse<RunningDto.StartResponse> start() {
        return ApiResponse.ok(runningService.start(SecurityUtil.currentUserId()));
    }

    /** 러닝 종료 데이터 수신 */
    @Operation(summary = "러닝 종료 (리포트 생성 + 후속 처리)",
            description = "하나의 트랜잭션에서 저장 + 스탯 반영 + 퀘스트 진행도 + 업적 판정 + 크루 누적 거리 처리. "
                    + "clientRunId 중복은 기존 기록 반환(idempotent=true), 거리 0.03km 미만은 폐기(discarded=true, 리포트 미생성). "
                    + "잘못된 데이터 RUNNING_001, 미래 시각 RUNNING_002. "
                    + "응답에 스탯 변화량 + 이번 러닝으로 달성된 퀘스트/업적 요약 포함")
    @PostMapping("/api/runnings/complete")
    public ApiResponse<RunningDto.CompleteResponse> complete(
            @Valid @RequestBody RunningDto.CompleteRequest request) {
        return ApiResponse.ok(runningService.complete(SecurityUtil.currentUserId(), request));
    }

    /** 리포트 상세 조회 */
    @Operation(summary = "리포트 상세 조회",
            description = "전 지표를 개별 필드로 반환(꾸미기 위젯용) + 구간 페이스 배열 + 스냅샷 기반 강아지 정보(당시 이름/외형). "
                    + "심박수는 null 그대로(프론트 - 표시). 조회 시 is_checked 읽음 처리")
    @GetMapping("/api/runnings/{recordId}/report")
    public ApiResponse<RunningDto.ReportResponse> getReport(@PathVariable Long recordId) {
        return ApiResponse.ok(runningService.getReport(SecurityUtil.currentUserId(), recordId));
    }

    /** 월별 히스토리 */
    @Operation(summary = "월별 히스토리",
            description = "월 요약(누적 거리/러닝 횟수/총 시간) + 해당 월 기록 목록(미확인 여부 checked 포함)")
    @GetMapping("/api/runnings/history")
    public ApiResponse<RunningDto.HistoryResponse> getHistory(
            @RequestParam int year, @RequestParam int month) {
        return ApiResponse.ok(runningService.getHistory(SecurityUtil.currentUserId(), year, month));
    }

    /** 미확인 리포트 존재 여부 */
    @Operation(summary = "미확인 리포트 존재 여부", description = "홈 화면 기록 메뉴 빨간 점 표시용")
    @GetMapping("/api/runnings/unchecked-exists")
    public ApiResponse<RunningDto.UncheckedExists> uncheckedExists() {
        return ApiResponse.ok(new RunningDto.UncheckedExists(
                runningService.uncheckedExists(SecurityUtil.currentUserId())));
    }

    /** 꾸미기 스티커 목록 */
    @Operation(summary = "꾸미기 스티커 목록", description = "히스토리 꾸미기용 스티커 마스터. 편집 결과물은 서버에 저장하지 않는다")
    @GetMapping("/api/stickers")
    public ApiResponse<List<RunningDto.StickerItem>> getStickers() {
        return ApiResponse.ok(runningService.getStickers());
    }
}

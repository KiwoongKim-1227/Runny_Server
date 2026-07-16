package com.goodspace.runny.domain.quest.controller;

import com.goodspace.runny.domain.quest.dto.QuestDto;
import com.goodspace.runny.domain.quest.service.QuestService;
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
 * 퀘스트 API 컨트롤러. 오늘의 퀘스트 조회, 보상 수령, 꾸미기 이벤트를 제공한다.
 */
@Tag(name = "Quest", description = "퀘스트 API - 일일 고정/랜덤 + 주간 퀘스트, 수령 버튼 방식 보상")
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;

    /** 오늘의 퀘스트 조회 */
    @Operation(summary = "오늘의 퀘스트 조회",
            description = "고정 3 + 랜덤 2(daily) + 주간 2(weekly). 진행도/달성(completed)/수령(claimed) 여부 포함. "
                    + "없으면 조회 시점 lazy 생성(랜덤 2종 + 수치 확정 스냅샷), 조회 시 접속하기 퀘스트 자동 달성")
    @GetMapping("/today")
    public ApiResponse<QuestDto.TodayResponse> getToday() {
        return ApiResponse.ok(questService.getToday(SecurityUtil.currentUserId()));
    }

    /** 퀘스트 보상 수령 */
    @Operation(summary = "퀘스트 보상 수령",
            description = "달성 상태에서만 가능. 경험치는 활성 강아지에게 적립(레벨업 처리 + 변화 로그 기록), 코인은 계정 지급. "
                    + "미달성 QUEST_001, 중복 수령 QUEST_002, 만료(전일/전주) QUEST_003")
    @PostMapping("/{userQuestId}/claim")
    public ApiResponse<QuestDto.ClaimResponse> claim(@PathVariable Long userQuestId) {
        return ApiResponse.ok(questService.claim(SecurityUtil.currentUserId(), userQuestId));
    }

    /** 히스토리 꾸미기 완료 이벤트 */
    @Operation(summary = "히스토리 꾸미기 완료 이벤트",
            description = "꾸미기 데이터 다운로드 완료 시 프론트가 호출. 랜덤 퀘스트(DECORATE) 진행도 갱신")
    @PostMapping("/events/decorate")
    public ApiResponse<Void> decorateEvent() {
        questService.onDecorateEvent(SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }
}

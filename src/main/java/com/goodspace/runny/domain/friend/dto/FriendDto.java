package com.goodspace.runny.domain.friend.dto;

import com.goodspace.runny.domain.friend.entity.RelationStatus;
import com.goodspace.runny.domain.user.dto.UserSummary;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 친구/놀이터 요청/응답 DTO 모음. 유저 정보는 UserSummary 공통 DTO를 재사용한다.
 */
public final class FriendDto {

    private FriendDto() {
    }

    /** 친구 요청 보내기 */
    public record RequestSend(
            @NotNull Long targetUserId
    ) {
    }

    /** 친구 목록 항목 - 같이 놀고 있는(놀이터 초대된) 친구 최상단 정렬 + 플래그 */
    public record FriendItem(
            UserSummary user,
            boolean isPlayingTogether
    ) {
    }

    /** 친구 상세 - 친구 강아지 프로필 팝업용 (UserSummary + 강아지 스탯) */
    public record FriendDetail(
            UserSummary user,
            int stamina,
            int endurance,
            int speed
    ) {
    }

    /** 검색 결과 항목 - 나와의 관계 상태 포함 (프론트 버튼 분기) */
    public record SearchItem(
            UserSummary user,
            RelationStatus relationStatus
    ) {
    }

    /** 검색 응답 - 닉네임 부분 일치, 전체 반환 (MVP 규모에서 페이징 제거) */
    public record SearchResponse(
            List<SearchItem> content
    ) {
    }

    /** 요청 목록 항목 - 보낸/받은 요청 공용 (상대 유저 요약 포함) */
    public record RequestItem(
            Long requestId,
            UserSummary user
    ) {
    }

    /** 받은 요청 목록 응답 - 미확인 존재 여부 포함 (조회 시 확인 처리됨) */
    public record ReceivedRequests(
            boolean hadUnchecked,
            List<RequestItem> requests
    ) {
    }

    /** 놀이터 초대 저장 요청 - 친구 ID 배열 전체 교체 (최대 4명) */
    public record InviteSaveRequest(
            @NotNull List<Long> friendUserIds
    ) {
    }

    /** 놀이터 메인 응답 - 내 강아지 + 초대 강아지 + 보유 코인 + 프로필 배지 */
    public record PlaygroundResponse(
            UserSummary me,
            List<UserSummary> invitedFriends,
            int coin,
            boolean hasDogProfileBadge
    ) {
    }
}

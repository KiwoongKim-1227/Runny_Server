package com.goodspace.runny.domain.friend.service;

import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.domain.friend.dto.FriendDto;
import com.goodspace.runny.domain.friend.entity.PlaygroundInvite;
import com.goodspace.runny.domain.friend.repository.PlaygroundInviteRepository;
import com.goodspace.runny.domain.user.dto.UserSummary;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.domain.user.service.UserSummaryService;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 놀이터 서비스. 초대 목록 전체 교체 저장과 놀이터 메인 조회(내 강아지 + 초대 강아지 + 코인 + 배지)를 담당한다.
 * 초대는 초대한 사용자에게만 보이는 개인 설정이며 상대에게 알림/노출되지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PlaygroundService {

    // 놀이터 초대 최대 인원 (내 강아지 포함 최대 5마리)
    private static final int MAX_INVITES = 4;

    private final PlaygroundInviteRepository playgroundInviteRepository;
    private final UserRepository userRepository;
    private final UserSummaryService userSummaryService;
    private final FriendService friendService;
    private final DogService dogService;

    /** 초대 목록 저장 - 친구 ID 배열(최대 4명) 전체 교체(PUT). 친구 관계를 전건 검증한다 */
    @Transactional
    public void saveInvites(Long userId, FriendDto.InviteSaveRequest request) {
        List<Long> friendIds = List.copyOf(new LinkedHashSet<>(request.friendUserIds()));
        if (friendIds.size() > MAX_INVITES) {
            throw new BusinessException(ErrorCode.FRIEND_005);
        }
        friendIds.forEach(friendId -> friendService.assertFriends(userId, friendId));

        // 전체 교체: 기존 초대 삭제 후 재등록
        playgroundInviteRepository.deleteByOwnerId(userId);
        friendIds.forEach(friendId ->
                playgroundInviteRepository.save(new PlaygroundInvite(userId, friendId)));
    }

    /** 놀이터 메인 조회 - 내 활성 강아지(착용 외형 포함) + 초대 친구 강아지들 + 보유 코인 + 프로필 배지 */
    @Transactional(readOnly = true)
    public FriendDto.PlaygroundResponse getPlayground(Long userId) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));

        List<Long> invitedIds = playgroundInviteRepository.findByOwnerIdOrderByIdAsc(userId).stream()
                .map(PlaygroundInvite::getFriendUserId)
                .toList();

        // 내 요약 + 초대 친구 요약 일괄 조립 (친구 삭제/탈퇴 시 초대는 함께 제거되지만 방어적으로 필터)
        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(
                java.util.stream.Stream.concat(java.util.stream.Stream.of(userId), invitedIds.stream()).toList());
        List<UserSummary> invitedFriends = invitedIds.stream()
                .map(summaries::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        // hasDogProfileBadge: 활성 강아지의 미확인 변경 존재 여부 (3단계 dog_change_log 기반)
        boolean hasDogProfileBadge = dogService.hasUnseenChangesForActiveDog(userId);

        return new FriendDto.PlaygroundResponse(
                summaries.get(userId), invitedFriends, me.getCoin(), hasDogProfileBadge);
    }
}

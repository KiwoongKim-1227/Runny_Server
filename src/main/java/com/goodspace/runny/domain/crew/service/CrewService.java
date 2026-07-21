package com.goodspace.runny.domain.crew.service;

import com.goodspace.runny.domain.crew.dto.CrewDto;
import com.goodspace.runny.domain.crew.entity.Crew;
import com.goodspace.runny.domain.crew.entity.CrewJoinRequest;
import com.goodspace.runny.domain.crew.entity.CrewMember;
import com.goodspace.runny.domain.crew.entity.CrewRole;
import com.goodspace.runny.domain.crew.entity.JoinRequestStatus;
import com.goodspace.runny.domain.crew.repository.CrewJoinRequestRepository;
import com.goodspace.runny.domain.crew.repository.CrewMemberRepository;
import com.goodspace.runny.domain.crew.repository.CrewRepository;
import com.goodspace.runny.domain.user.dto.UserSummary;
import com.goodspace.runny.domain.user.service.UserSummaryService;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 크루 서비스. 크루명 중복확인, 생성, 검색, 상세, 내 크루 조회, 가입 신청/취소, 탈퇴를 담당한다.
 * 크루장 전용 관리 기능은 CrewAdminService가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CrewService {

    // S3 크루 로고 프리픽스 (문서 8.4)
    static final String IMAGE_PREFIX = "crew/";

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final CrewValidator crewValidator;
    private final CrewWeeklyDistanceProvider weeklyDistanceProvider;
    private final UserSummaryService userSummaryService;
    private final S3Uploader s3Uploader;
    private final CrewNotificationHook crewNotificationHook;

    /** 크루명 사용 가능 여부 - 형식/비속어 위반은 예외, 중복이면 false */
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name) {
        crewValidator.validateNameFormat(name);
        return !crewRepository.existsByName(name);
    }

    /** 크루 생성 - 로고는 선택(있으면 S3 crew/ 업로드, 없으면 기본 이미지), 생성자는 LEADER */
    @Transactional
    public Long create(Long userId, String name, String intro, MultipartFile image) {
        if (crewMemberRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.CREW_005);
        }
        crewValidator.validateName(name);
        crewValidator.validateIntro(intro);

        // S3 업로드는 트랜잭션 보호를 받지 못하므로, 업로드 후 DB 저장 실패 시 새 객체를 보상 삭제한다 (고아 객체 방지)
        String imageUrl = (image == null || image.isEmpty()) ? null : s3Uploader.upload(image, IMAGE_PREFIX);
        try {
            Crew crew = crewRepository.save(new Crew(name, imageUrl, intro, userId));
            crewMemberRepository.save(new CrewMember(crew.getId(), userId, CrewRole.LEADER));
            // 크루 소속이 되었으므로 다른 크루에 보낸 대기 중 신청은 정리
            crewJoinRequestRepository.deleteAllByUserId(userId);
            // 크루 생성자도 크루 가입으로 보고 "친구들과 달리기" 업적 판정
            crewNotificationHook.onCrewCreated(userId);
            return crew.getId();
        } catch (RuntimeException e) {
            if (imageUrl != null) {
                s3Uploader.delete(imageUrl);
            }
            throw e;
        }
    }

    /** 크루 검색 - 부분 일치, 전체 반환(MVP 규모 페이징 제거). memberCount와 myRequestStatus(NONE/PENDING) 포함 */
    @Transactional(readOnly = true)
    public CrewDto.SearchResponse search(Long userId, String name) {
        List<Crew> crews = crewRepository.findByNameContainingOrderByIdAsc(name);
        List<Long> crewIds = crews.stream().map(Crew::getId).toList();

        // 현재 인원 일괄 집계 + 내 대기 중 신청 크루 목록
        Map<Long, Integer> memberCounts = new HashMap<>();
        if (!crewIds.isEmpty()) {
            crewMemberRepository.countByCrewIds(crewIds).forEach(row ->
                    memberCounts.put((Long) row[0], ((Long) row[1]).intValue()));
        }
        List<Long> myPendingCrewIds = crewJoinRequestRepository.findPendingCrewIdsOf(userId);

        List<CrewDto.SearchItem> content = crews.stream()
                .map(crew -> new CrewDto.SearchItem(
                        crew.getId(), crew.getName(), crew.displayImageUrl(), crew.getIntro(),
                        memberCounts.getOrDefault(crew.getId(), 0), crew.getMaxMembers(),
                        myPendingCrewIds.contains(crew.getId()) ? "PENDING" : "NONE"))
                .toList();
        return new CrewDto.SearchResponse(content);
    }

    /** 크루 상세 - 이미지/이름/한줄소개/총 누적 거리/멤버 수(현재·최대)/이번 주 top3/크루원 목록 */
    @Transactional(readOnly = true)
    public CrewDto.DetailResponse getDetail(Long crewId) {
        Crew crew = findCrew(crewId);
        List<CrewMember> members = crewMemberRepository.findByCrewIdOrderByJoinedAtAsc(crewId);

        // 멤버 요약 일괄 조립 (6단계 UserSummaryService 재사용)
        List<Long> memberUserIds = members.stream().map(CrewMember::getUserId).toList();
        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(memberUserIds);
        List<CrewDto.MemberItem> memberItems = members.stream()
                .filter(member -> summaries.containsKey(member.getUserId()))
                .map(member -> new CrewDto.MemberItem(summaries.get(member.getUserId()), member.getRole()))
                .toList();

        // 이번 주 top3 (월요일 00:00 KST 기준, running_record는 9단계 - 현재는 빈 목록)
        List<CrewWeeklyDistanceProvider.MemberDistance> top3 = weeklyDistanceProvider.weeklyTop3(crewId);
        List<CrewDto.TopMember> topMembers = new java.util.ArrayList<>();
        for (int i = 0; i < top3.size(); i++) {
            CrewWeeklyDistanceProvider.MemberDistance entry = top3.get(i);
            UserSummary summary = summaries.get(entry.userId());
            if (summary != null) {
                topMembers.add(new CrewDto.TopMember(i + 1, summary, entry.distanceKm()));
            }
        }

        return new CrewDto.DetailResponse(crew.getId(), crew.getName(), crew.displayImageUrl(),
                crew.getIntro(), crew.getTotalDistance(), members.size(), crew.getMaxMembers(),
                topMembers, memberItems);
    }

    /** 내 크루 조회 - role 포함, 크루장이면 pendingRequestCount(대기 중 가입 신청 수) 포함 */
    @Transactional(readOnly = true)
    public CrewDto.MyCrewResponse getMyCrew(Long userId) {
        return crewMemberRepository.findByUserId(userId)
                .map(member -> {
                    Crew crew = findCrew(member.getCrewId());
                    Integer pendingCount = member.getRole() == CrewRole.LEADER
                            ? crewJoinRequestRepository.countByCrewIdAndStatus(crew.getId(), JoinRequestStatus.PENDING)
                            : null;
                    return CrewDto.MyCrewResponse.of(crew, member.getRole(), pendingCount);
                })
                .orElse(CrewDto.MyCrewResponse.none());
    }

    /** 가입 신청 - 1인 1크루, 중복 신청 불가, 정원 초과 크루는 신청 불가. 정원 검증은 크루 행 락으로 직렬화 */
    @Transactional
    public void requestJoin(Long userId, Long crewId) {
        if (crewMemberRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.CREW_005);
        }
        Crew crew = findCrewForUpdate(crewId);
        if (crewJoinRequestRepository.existsByCrewIdAndUserIdAndStatus(crewId, userId, JoinRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CREW_007);
        }
        if (crewMemberRepository.countByCrewId(crewId) >= crew.getMaxMembers()) {
            throw new BusinessException(ErrorCode.CREW_008);
        }
        crewJoinRequestRepository.save(new CrewJoinRequest(crewId, userId));
    }

    /** 가입 신청 취소 - 본인 PENDING 신청만 */
    @Transactional
    public void cancelJoin(Long userId, Long crewId) {
        CrewJoinRequest request = crewJoinRequestRepository
                .findByCrewIdAndUserIdAndStatus(crewId, userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_012));
        crewJoinRequestRepository.delete(request);
    }

    /** 크루 탈퇴 - 일반 크루원만 가능, 크루장은 위임 또는 해체 후 탈퇴 */
    @Transactional
    public void leave(Long userId, Long crewId) {
        CrewMember member = crewMemberRepository.findByCrewIdAndUserId(crewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_011));
        if (member.getRole() == CrewRole.LEADER) {
            throw new BusinessException(ErrorCode.CREW_010);
        }
        crewMemberRepository.delete(member);
    }

    /**
     * 회원 탈퇴 훅 (UserService에서 호출) - 크루장이고 크루원이 있으면 위임 필요 에러,
     * 크루장 혼자면 크루 해체(S3 로고 삭제 포함), 일반 크루원이면 멤버십 삭제. 본인 가입 신청도 전부 정리한다.
     */
    @Transactional
    public void handleUserWithdrawal(Long userId) {
        crewMemberRepository.findByUserId(userId).ifPresent(member -> {
            if (member.getRole() == CrewRole.LEADER) {
                int memberCount = crewMemberRepository.countByCrewId(member.getCrewId());
                if (memberCount > 1) {
                    throw new BusinessException(ErrorCode.CREW_010);
                }
                // 크루장 혼자인 크루는 해체 후 탈퇴 (임의 설정, 문서 4.A)
                Crew crew = findCrew(member.getCrewId());
                String imageUrl = crew.getImageUrl();
                crewJoinRequestRepository.deleteAllByCrewId(member.getCrewId());
                crewMemberRepository.delete(member);
                crewRepository.deleteById(member.getCrewId());
                // S3 로고 삭제 - 커밋 후 수행 (문서 8.4 삭제 정책)
                if (imageUrl != null) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            s3Uploader.delete(imageUrl);
                        }
                    });
                }
            } else {
                crewMemberRepository.delete(member);
            }
        });
        crewJoinRequestRepository.deleteAllByUserId(userId);
    }

    /** 크루 조회 (비관적 쓰기 락) - 정원 검증/변경 흐름에서 사용. 동시 승인/신청 간 정원 초과 방지 */
    Crew findCrewForUpdate(Long crewId) {
        return crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_006));
    }

    /** 크루 조회 공통 */
    Crew findCrew(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_006));
    }
}

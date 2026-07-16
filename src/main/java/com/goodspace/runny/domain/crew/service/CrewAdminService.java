package com.goodspace.runny.domain.crew.service;

import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.service.CoinService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 크루장 전용 관리 서비스. 가입 신청 목록/일괄 승인·거절, 추방, 해체, 크루명 변경(1,000코인),
 * 한줄소개 변경(무료), 정원 확장(1,000코인당 +50), 이미지 변경, 크루장 위임을 담당한다.
 * 모든 기능은 크루장 권한 검증 공통 메서드(validateLeader)를 거친다.
 */
@Service
@RequiredArgsConstructor
public class CrewAdminService {

    private static final int NAME_CHANGE_COST = 1_000;
    private static final int CAPACITY_EXPAND_COST = 1_000;

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final CrewService crewService;
    private final CrewValidator crewValidator;
    private final CrewNotificationHook crewNotificationHook;
    private final UserSummaryService userSummaryService;
    private final CoinService coinService;
    private final S3Uploader s3Uploader;

    /** 가입 신청 목록 - PENDING만, 신청자는 UserSummary(강아지 외형 포함) */
    @Transactional(readOnly = true)
    public List<CrewDto.JoinRequestItem> getJoinRequests(Long crewId, Long leaderId) {
        validateLeader(crewId, leaderId);
        List<CrewJoinRequest> requests = crewJoinRequestRepository
                .findByCrewIdAndStatusOrderByIdAsc(crewId, JoinRequestStatus.PENDING);
        List<Long> userIds = requests.stream().map(CrewJoinRequest::getUserId).toList();
        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(userIds);
        return requests.stream()
                .filter(request -> summaries.containsKey(request.getUserId()))
                .map(request -> new CrewDto.JoinRequestItem(request.getId(), summaries.get(request.getUserId())))
                .toList();
    }

    /** 일괄 승인 - 건별 정원 재검증, 초과분은 실패 처리 후 결과 반환. 승인 성공 건은 알림 훅 호출 */
    @Transactional
    public CrewDto.BatchResult approveAll(Long crewId, Long leaderId, List<Long> requestIds) {
        // 정원 재검증이 있으므로 크루 행 락으로 동시 승인/신청과 직렬화
        Crew crew = validateLeaderForUpdate(crewId, leaderId);
        List<Long> succeeded = new ArrayList<>();
        List<CrewDto.BatchResult.FailedItem> failed = new ArrayList<>();

        int currentCount = crewMemberRepository.countByCrewId(crewId);
        for (Long requestId : requestIds) {
            CrewJoinRequest request = findPendingRequest(crewId, requestId, failed);
            if (request == null) {
                continue;
            }
            // 승인 시점 정원 재검증
            if (currentCount >= crew.getMaxMembers()) {
                failed.add(new CrewDto.BatchResult.FailedItem(requestId, "크루 정원이 가득 찼습니다."));
                continue;
            }
            // 신청자가 그 사이 다른 크루에 가입한 경우 (1인 1크루)
            if (crewMemberRepository.existsByUserId(request.getUserId())) {
                request.reject();
                failed.add(new CrewDto.BatchResult.FailedItem(requestId, "이미 다른 크루에 소속된 유저입니다."));
                continue;
            }
            request.approve();
            crewMemberRepository.save(new CrewMember(crewId, request.getUserId(), CrewRole.MEMBER));
            crewJoinRequestRepository.deleteAllByUserId(request.getUserId());
            currentCount++;
            succeeded.add(requestId);
            crewNotificationHook.onJoinApproved(request.getUserId(), crewId, crew.getName());
        }
        return new CrewDto.BatchResult(succeeded, failed);
    }

    /** 일괄 거절 - 처리된 요청은 PENDING 목록에서 자동 제외된다 */
    @Transactional
    public CrewDto.BatchResult rejectAll(Long crewId, Long leaderId, List<Long> requestIds) {
        validateLeader(crewId, leaderId);
        List<Long> succeeded = new ArrayList<>();
        List<CrewDto.BatchResult.FailedItem> failed = new ArrayList<>();
        for (Long requestId : requestIds) {
            CrewJoinRequest request = findPendingRequest(crewId, requestId, failed);
            if (request == null) {
                continue;
            }
            request.reject();
            succeeded.add(requestId);
        }
        return new CrewDto.BatchResult(succeeded, failed);
    }

    /** 크루원 추방 - 자기 자신 추방 불가 */
    @Transactional
    public void kick(Long crewId, Long leaderId, Long targetUserId) {
        validateLeader(crewId, leaderId);
        if (leaderId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CREW_013);
        }
        CrewMember target = crewMemberRepository.findByCrewIdAndUserId(crewId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_014));
        crewMemberRepository.delete(target);
    }

    /** 크루 해체 - 멤버십/신청 전부 삭제, 로고 S3 삭제는 커밋 후 수행 */
    @Transactional
    public void disband(Long crewId, Long leaderId) {
        Crew crew = validateLeader(crewId, leaderId);
        String imageUrl = crew.getImageUrl();
        crewMemberRepository.deleteAllByCrewId(crewId);
        crewJoinRequestRepository.deleteAllByCrewId(crewId);
        crewRepository.delete(crew);
        deleteImageAfterCommit(imageUrl);
    }

    /**
     * 크루명 변경 - 1,000코인 차감 + 8자/중복/비속어 재검증.
     * 검증 순서: 형식/비속어 -> 현재와 동일명이면 차감 없이 조기 반환 -> 중복 -> 코인 차감 -> 변경.
     * 모든 검증을 차감보다 앞에 두어 검증 실패 시 코인이 차감되지 않게 한다 (같은 트랜잭션이지만 순서로 의도를 명확화).
     */
    @Transactional
    public void changeName(Long crewId, Long leaderId, String newName) {
        Crew crew = validateLeader(crewId, leaderId);
        crewValidator.validateNameFormat(newName);
        if (newName.equals(crew.getName())) {
            return;
        }
        crewValidator.validateName(newName);
        coinService.deduct(leaderId, NAME_CHANGE_COST, CoinTransactionType.CREW_SPEND, crewId);
        crew.changeName(newName);
    }

    /** 한줄소개 변경 - 무료, 30자 제한 */
    @Transactional
    public void changeIntro(Long crewId, Long leaderId, String intro) {
        Crew crew = validateLeader(crewId, leaderId);
        crewValidator.validateIntro(intro);
        crew.changeIntro(intro);
    }

    /** 정원 확장 - 1,000코인당 +50 */
    @Transactional
    public int expandCapacity(Long crewId, Long leaderId) {
        Crew crew = validateLeaderForUpdate(crewId, leaderId);
        coinService.deduct(leaderId, CAPACITY_EXPAND_COST, CoinTransactionType.CREW_SPEND, crewId);
        crew.expandCapacity();
        return crew.getMaxMembers();
    }

    /** 이미지 변경 - 신규 업로드 후 기존 객체는 커밋 후 삭제 (문서 8.4 삭제 정책) */
    @Transactional
    public String changeImage(Long crewId, Long leaderId, MultipartFile image) {
        Crew crew = validateLeader(crewId, leaderId);
        String oldImageUrl = crew.getImageUrl();
        // 업로드 성공 후 DB 반영 실패 시 새 객체를 보상 삭제해 고아 객체를 남기지 않는다
        String newImageUrl = s3Uploader.upload(image, CrewService.IMAGE_PREFIX);
        try {
            crew.changeImage(newImageUrl);
            crewRepository.flush();
        } catch (RuntimeException e) {
            s3Uploader.delete(newImageUrl);
            throw e;
        }
        deleteImageAfterCommit(oldImageUrl);
        return newImageUrl;
    }

    /** 크루장 위임 - 상대 수락 없이 즉시, 본인은 MEMBER로 전환 */
    @Transactional
    public void delegateLeader(Long crewId, Long leaderId, Long targetUserId) {
        Crew crew = validateLeader(crewId, leaderId);
        if (leaderId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CREW_013);
        }
        CrewMember target = crewMemberRepository.findByCrewIdAndUserId(crewId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_014));
        CrewMember me = crewMemberRepository.findByCrewIdAndUserId(crewId, leaderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_011));

        target.changeRole(CrewRole.LEADER);
        me.changeRole(CrewRole.MEMBER);
        crew.changeLeader(targetUserId);
    }

    /** 크루장 권한 검증 공통 메서드 - 크루 존재 + leader_id 일치 확인 */
    private Crew validateLeader(Long crewId, Long userId) {
        return assertLeader(crewService.findCrew(crewId), userId);
    }

    /** 크루장 권한 검증 (비관적 락) - 정원 검증이 있는 흐름(일괄 승인/정원 확장)용 */
    private Crew validateLeaderForUpdate(Long crewId, Long userId) {
        return assertLeader(crewService.findCrewForUpdate(crewId), userId);
    }

    /** leader_id 일치 검증 공통 */
    private Crew assertLeader(Crew crew, Long userId) {
        if (!crew.getLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.CREW_009);
        }
        return crew;
    }

    /** PENDING 신청 조회 - 대상이 아니면 실패 목록에 사유 추가 후 null 반환 (일괄 처리용) */
    private CrewJoinRequest findPendingRequest(Long crewId, Long requestId,
                                               List<CrewDto.BatchResult.FailedItem> failed) {
        return crewJoinRequestRepository.findById(requestId)
                .filter(request -> request.getCrewId().equals(crewId))
                .filter(request -> request.getStatus() == JoinRequestStatus.PENDING)
                .orElseGet(() -> {
                    failed.add(new CrewDto.BatchResult.FailedItem(requestId, "존재하지 않거나 이미 처리된 신청입니다."));
                    return null;
                });
    }

    /**
     * S3 이미지 삭제를 트랜잭션 커밋 후 수행. DB와 S3의 원자성은 보장되지 않으므로
     * 삭제 실패 시에도 서비스 흐름은 계속하고 고아 객체는 추후 정리 배치 대상으로 남긴다 (문서 8.4).
     */
    private void deleteImageAfterCommit(String imageUrl) {
        if (imageUrl == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3Uploader.delete(imageUrl);
            }
        });
    }
}

package com.goodspace.runny.domain.setting.service;

import com.goodspace.runny.domain.dog.repository.UserDogRepository;
import com.goodspace.runny.domain.setting.dto.SettingDto;
import com.goodspace.runny.domain.setting.entity.UserSetting;
import com.goodspace.runny.domain.setting.repository.UserSettingRepository;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설정 서비스. 설정 메인(강아지 사진/이름, 이메일, provider)과 알림 토글 조회/변경을 담당한다.
 * 나머지 설정 항목(개인정보 수정/비밀번호 변경/탈퇴/로그아웃)은 2단계 회원 API를 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class SettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;
    private final UserDogRepository userDogRepository;

    /** 설정 메인 - 현재 활성 강아지 모델(glb)/이름 + 이메일 + provider (비밀번호 변경 메뉴 노출 분기용) */
    @Transactional(readOnly = true)
    public SettingDto.MeResponse getSettingMain(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_003));
        String dogName = null;
        String dogModelUrl = null;
        if (user.getActiveDogId() != null) {
            var dog = userDogRepository.findById(user.getActiveDogId()).orElse(null);
            if (dog != null) {
                dogName = dog.getName();
                dogModelUrl = dog.getBreed().getModelUrl();
            }
        }
        return new SettingDto.MeResponse(dogName, dogModelUrl, user.getEmail(), user.getProvider());
    }

    /** 알림 설정 조회 - 없으면 기본값(전부 on)으로 lazy 생성 */
    @Transactional
    public SettingDto.NotificationSettings getNotificationSettings(Long userId) {
        UserSetting setting = getOrCreate(userId);
        return new SettingDto.NotificationSettings(
                setting.isNotifyFriendRequest(), setting.isNotifyCrewApproval());
    }

    /** 알림 설정 변경 - null 필드는 미변경 */
    @Transactional
    public SettingDto.NotificationSettings updateNotificationSettings(
            Long userId, SettingDto.UpdateRequest request) {
        UserSetting setting = getOrCreate(userId);
        setting.update(request.notifyFriendRequest(), request.notifyCrewApproval());
        return new SettingDto.NotificationSettings(
                setting.isNotifyFriendRequest(), setting.isNotifyCrewApproval());
    }

    /** 설정 조회/생성 공통 - 알림 생성 시 토글 확인에도 사용 (NotificationService에서 호출) */
    @Transactional
    public UserSetting getOrCreate(Long userId) {
        return userSettingRepository.findByUserId(userId)
                .orElseGet(() -> userSettingRepository.save(new UserSetting(userId)));
    }
}

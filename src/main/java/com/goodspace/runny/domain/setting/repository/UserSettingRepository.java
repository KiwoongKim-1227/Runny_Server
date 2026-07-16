package com.goodspace.runny.domain.setting.repository;

import com.goodspace.runny.domain.setting.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 알림 설정 리포지토리.
 */
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    Optional<UserSetting> findByUserId(Long userId);
}

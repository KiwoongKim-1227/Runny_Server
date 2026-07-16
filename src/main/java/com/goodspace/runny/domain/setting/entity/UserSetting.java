package com.goodspace.runny.domain.setting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 설정 엔티티. 친구 요청/크루 승인 알림 토글, 기본값 on. 최초 접근 시 lazy 생성한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_setting_user", columnNames = "user_id")
})
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notify_friend_request", nullable = false)
    private boolean notifyFriendRequest;

    @Column(name = "notify_crew_approval", nullable = false)
    private boolean notifyCrewApproval;

    public UserSetting(Long userId) {
        this.userId = userId;
        this.notifyFriendRequest = true;
        this.notifyCrewApproval = true;
    }

    /** 토글 변경 - null은 미변경 */
    public void update(Boolean notifyFriendRequest, Boolean notifyCrewApproval) {
        if (notifyFriendRequest != null) {
            this.notifyFriendRequest = notifyFriendRequest;
        }
        if (notifyCrewApproval != null) {
            this.notifyCrewApproval = notifyCrewApproval;
        }
    }
}

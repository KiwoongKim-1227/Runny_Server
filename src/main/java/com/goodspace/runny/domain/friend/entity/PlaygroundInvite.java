package com.goodspace.runny.domain.friend.entity;

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
 * 놀이터 초대 엔티티. 초대는 초대한 사용자(owner)에게만 보이는 개인 설정이며 상대에게 노출되지 않는다.
 * owner당 최대 4행, (owner+friend) UNIQUE.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "playground_invite", uniqueConstraints = {
        @UniqueConstraint(name = "uk_playground_invite", columnNames = {"owner_id", "friend_user_id"})
})
public class PlaygroundInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "friend_user_id", nullable = false)
    private Long friendUserId;

    public PlaygroundInvite(Long ownerId, Long friendUserId) {
        this.ownerId = ownerId;
        this.friendUserId = friendUserId;
    }
}

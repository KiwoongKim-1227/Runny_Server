package com.goodspace.runny.domain.notification.repository;

import com.goodspace.runny.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 알림 리포지토리. 미읽음 존재 여부(빨간 점)와 목록 조회 시 일괄 읽음 처리를 지원한다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByIdDesc(Long userId);

    boolean existsByUserIdAndReadFalse(Long userId);

    /** 목록 조회 시 일괄 읽음 처리 */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllRead(@Param("userId") Long userId);
}

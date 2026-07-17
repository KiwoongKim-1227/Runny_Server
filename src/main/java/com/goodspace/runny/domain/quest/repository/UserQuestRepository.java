package com.goodspace.runny.domain.quest.repository;

import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.entity.UserQuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 유저 퀘스트 리포지토리. period_key 기준으로 오늘/이번 주 퀘스트를 조회한다.
 */
public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {

    @Query("SELECT uq FROM UserQuest uq JOIN FETCH uq.quest WHERE uq.userId = :userId AND uq.periodKey = :periodKey")
    List<UserQuest> findByUserIdAndPeriodKey(@Param("userId") Long userId, @Param("periodKey") String periodKey);

    /** 진행도 갱신 대상 조회 - 현재 기간 + 조건 유형 일치 + 미수령 */
    @Query("SELECT uq FROM UserQuest uq JOIN FETCH uq.quest q " +
            "WHERE uq.userId = :userId AND uq.periodKey = :periodKey " +
            "AND q.conditionType = :conditionType AND uq.claimed = false")
    List<UserQuest> findForProgress(@Param("userId") Long userId,
                                    @Param("periodKey") String periodKey,
                                    @Param("conditionType") QuestConditionType conditionType);

    Optional<UserQuest> findByIdAndUserId(Long id, Long userId);

    /**
     * 보상 수령 조건부 처리 - 영향 행 1인 요청만 보상 지급 주체 (더블클릭/동시 요청 중복 지급 차단).
     * clearAutomatically=true로 벌크 UPDATE 후 영속성 컨텍스트를 비워 stale 데이터 방지.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserQuest uq SET uq.claimed = true " +
            "WHERE uq.id = :id AND uq.userId = :userId AND uq.claimed = false")
    int claimIfNotClaimed(@Param("id") Long id, @Param("userId") Long userId);
}

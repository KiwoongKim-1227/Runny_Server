package com.goodspace.runny.domain.quest.dto;

import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.entity.QuestType;
import com.goodspace.runny.domain.quest.entity.UserQuest;

import java.util.List;

/**
 * 퀘스트 요청/응답 DTO 모음.
 */
public final class QuestDto {

    private QuestDto() {
    }

    /** 퀘스트 항목 - 진행도 바(예: 2.1/3km)와 취소선 렌더링은 프론트가 progress/target/completed로 처리 */
    public record QuestItem(
            Long userQuestId,
            QuestType type,
            QuestConditionType conditionType,
            String title,
            double targetValue,
            double progress,
            int rewardExp,
            int rewardCoin,
            boolean completed,
            boolean claimed
    ) {
        public static QuestItem from(UserQuest userQuest) {
            return new QuestItem(
                    userQuest.getId(),
                    userQuest.getQuest().getType(),
                    userQuest.getQuest().getConditionType(),
                    userQuest.getQuest().getTitle(),
                    userQuest.getTargetValue(),
                    userQuest.getProgress(),
                    userQuest.getQuest().getRewardExp(),
                    userQuest.getQuest().getRewardCoin(),
                    userQuest.isCompleted(),
                    userQuest.isClaimed()
            );
        }
    }

    /** 오늘의 퀘스트 응답 - 고정 3 + 랜덤 2 + 주간 2 */
    public record TodayResponse(
            List<QuestItem> daily,
            List<QuestItem> weekly
    ) {
    }

    /** 보상 수령 응답 - 지급된 경험치/코인과 레벨업 여부 */
    public record ClaimResponse(
            int grantedExp,
            int grantedCoin,
            int levelBefore,
            int levelAfter,
            boolean leveledUp
    ) {
    }
}

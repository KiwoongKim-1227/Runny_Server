package com.goodspace.runny.domain.quest.repository;

import com.goodspace.runny.domain.quest.entity.Quest;
import com.goodspace.runny.domain.quest.entity.QuestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 퀘스트 마스터 리포지토리.
 */
public interface QuestRepository extends JpaRepository<Quest, Long> {

    List<Quest> findByType(QuestType type);
}

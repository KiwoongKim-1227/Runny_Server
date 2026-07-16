package com.goodspace.runny.domain.quest.service;

import com.goodspace.runny.domain.quest.entity.Quest;
import com.goodspace.runny.domain.quest.entity.QuestConditionType;
import com.goodspace.runny.domain.quest.entity.QuestType;
import com.goodspace.runny.domain.quest.repository.QuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 퀘스트 마스터 시드 등록기. 일일 고정 3종 + 일일 랜덤 풀 4종 + 주간 2종 (문서 4.F 표).
 * 어떤 랜덤 조합이든 하루 합계 코인 50, 주간 합계 코인 200이 되도록 개당 보상이 설계되어 있다.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class QuestSeeder implements CommandLineRunner {

    private final QuestRepository questRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (questRepository.count() > 0) {
            return;
        }
        questRepository.saveAll(List.of(
                // 일일 고정 3종 (경험치만, 매일 00:00 초기화)
                new Quest(QuestType.DAILY_FIXED, "접속하기", QuestConditionType.LOGIN, 1, 1, 50, 0),
                new Quest(QuestType.DAILY_FIXED, "1km 달리기", QuestConditionType.DISTANCE, 1, 1, 100, 0),
                new Quest(QuestType.DAILY_FIXED, "오늘의 러닝 완료하기", QuestConditionType.RUN_COUNT, 1, 1, 150, 0),
                // 일일 랜덤 풀 4종 (매일 2개 랜덤 출제, 수치는 min~max 범위 내 랜덤 확정. 개당 100xp + 25코인)
                new Quest(QuestType.DAILY_RANDOM, "km 달리기", QuestConditionType.DISTANCE, 3, 6, 100, 25),
                new Quest(QuestType.DAILY_RANDOM, "분 이상 달리기", QuestConditionType.DURATION, 30, 60, 100, 25),
                new Quest(QuestType.DAILY_RANDOM, "분 안 쉬고 달리기", QuestConditionType.NONSTOP, 15, 30, 100, 25),
                new Quest(QuestType.DAILY_RANDOM, "오늘 러닝 완료 히스토리 꾸미기", QuestConditionType.DECORATE, 1, 1, 100, 25),
                // 주간 2종 (매주 월요일 초기화. 개당 300xp + 100코인)
                new Quest(QuestType.WEEKLY, "주 3회 러닝 완료하기", QuestConditionType.RUN_COUNT, 3, 3, 300, 100),
                new Quest(QuestType.WEEKLY, "누적 10km 이상 러닝하기", QuestConditionType.WEEK_DISTANCE, 10, 10, 300, 100)
        ));
        log.info("퀘스트 마스터 시드 9종 등록 완료");
    }
}

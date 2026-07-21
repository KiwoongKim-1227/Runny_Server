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
 * 퀘스트 마스터 시드 등록기 (최종 확정안).
 * 일일 고정 3종(경험치만) + 일일 랜덤 풀 7종(전부 100xp, 코인 개별) + 주간 풀 8종(한 주 랜덤 3종 출제).
 * 기존 데이터가 있으면 건너뛰므로, 확정안 반영 시 quest / user_quest 테이블을 비운 뒤 재기동해야 한다.
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
                new Quest(QuestType.DAILY_FIXED, "접속하기", QuestConditionType.LOGIN, 1, 1, 100, 0),
                new Quest(QuestType.DAILY_FIXED, "오늘의 러닝 완료하기", QuestConditionType.RUN_COUNT, 1, 1, 200, 0),
                new Quest(QuestType.DAILY_FIXED, "1km 이상 달리기", QuestConditionType.DISTANCE, 1, 1, 500, 0),
                // 일일 랜덤 풀 7종 (매일 2개 랜덤 출제, 전부 100xp + 코인 개별)
                new Quest(QuestType.DAILY_RANDOM, "히스토리 꾸미기 창 접속하기", QuestConditionType.DECORATE_ENTER, 1, 1, 100, 10),
                new Quest(QuestType.DAILY_RANDOM, "오늘 러닝 완료 사진 꾸미기", QuestConditionType.DECORATE, 1, 1, 100, 20),
                new Quest(QuestType.DAILY_RANDOM, "새로운 경로로 1km 이상 달리기", QuestConditionType.NEW_ROUTE, 1, 1, 100, 20),
                new Quest(QuestType.DAILY_RANDOM, "일정한 페이스로 2km 유지하기", QuestConditionType.STEADY_PACE, 2, 2, 100, 20),
                new Quest(QuestType.DAILY_RANDOM, "3km 달리기", QuestConditionType.DISTANCE, 3, 3, 100, 30),
                new Quest(QuestType.DAILY_RANDOM, "30분 이상 달리기", QuestConditionType.DURATION, 30, 30, 100, 30),
                new Quest(QuestType.DAILY_RANDOM, "15분 안 쉬고 달리기", QuestConditionType.NONSTOP, 15, 15, 100, 30),
                // 주간 풀 8종 (매주 월요일 초기화, 한 주 랜덤 3종 출제)
                new Quest(QuestType.WEEKLY, "주 3회 이상 앱 접속하여 1km 이상 러닝 완료하기", QuestConditionType.WEEK_RUN_1KM, 3, 3, 100, 60),
                new Quest(QuestType.WEEKLY, "일주일간 5일 이상 접속하기", QuestConditionType.WEEK_LOGIN_DAYS, 5, 5, 100, 60),
                new Quest(QuestType.WEEKLY, "주말 누적 7km 이상 달리기", QuestConditionType.WEEKEND_DISTANCE, 7, 7, 200, 70),
                new Quest(QuestType.WEEKLY, "일시정지 기능 사용하지 않고 30분 이상 달리기", QuestConditionType.NONSTOP, 30, 30, 200, 70),
                new Quest(QuestType.WEEKLY, "누적 15km 이상 러닝하기", QuestConditionType.WEEK_DISTANCE, 15, 15, 300, 80),
                new Quest(QuestType.WEEKLY, "이번 주 총 러닝 시간 100분 이상 달성하기", QuestConditionType.WEEK_DURATION, 100, 100, 300, 80),
                // 지난주 초과 퀘스트의 target은 출제 시점에 지난주 거리 스냅샷으로 확정 (마스터 값 0은 미사용)
                new Quest(QuestType.WEEKLY, "지난주 총 러닝 거리보다 많이 달리기", QuestConditionType.LAST_WEEK_BEAT, 0, 0, 300, 100),
                new Quest(QuestType.WEEKLY, "이번 주 누적 소모 칼로리 700kcal 이상 달성하기", QuestConditionType.WEEK_CALORIES, 700, 700, 300, 100)
        ));
        log.info("퀘스트 마스터 시드 18종 등록 완료 (고정 3 + 랜덤 풀 7 + 주간 풀 8)");
    }
}

package com.goodspace.runny.domain.crew.service;

import com.goodspace.runny.domain.crew.repository.CrewRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.util.ProfanityFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 크루 입력 검증 공용 컴포넌트. 크루명(1~8자 + 비속어 + 중복)과 한줄소개(30자) 규칙을
 * 생성/크루명 변경/중복확인에서 공통 사용한다.
 */
@Component
@RequiredArgsConstructor
public class CrewValidator {

    private static final int NAME_MAX_LENGTH = 8;
    private static final int INTRO_MAX_LENGTH = 30;

    private final CrewRepository crewRepository;
    private final ProfanityFilter profanityFilter;

    /** 크루명 형식(1~8자) + 비속어 검증 */
    public void validateNameFormat(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.CREW_002);
        }
        if (profanityFilter.containsProfanity(name)) {
            throw new BusinessException(ErrorCode.CREW_003);
        }
    }

    /** 크루명 형식 + 중복 검증 (생성/변경 저장 시점 재검증) */
    public void validateName(String name) {
        validateNameFormat(name);
        if (crewRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.CREW_001);
        }
    }

    /** 한줄소개 30자 제한 검증 (null/빈 값 허용) */
    public void validateIntro(String intro) {
        if (intro != null && intro.length() > INTRO_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.CREW_004);
        }
    }
}

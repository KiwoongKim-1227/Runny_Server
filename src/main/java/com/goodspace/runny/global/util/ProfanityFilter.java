package com.goodspace.runny.global.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 금칙어 목록 기반 비속어 검사 유틸. 닉네임/강아지 이름/크루명에 공통 적용한다.
 * 금칙어 목록은 필요 시 파일/DB 로딩 방식으로 교체 가능한 구조.
 */
@Component
public class ProfanityFilter {

    // 운영 시 별도 리소스 파일로 분리 권장. 여기서는 최소 예시 목록만 둔다.
    private static final List<String> BANNED_WORDS = List.of(
            "바보", "멍청이", "병신", "미친", "씨발", "시발", "개새끼", "새끼",
            "fuck", "shit", "bitch", "asshole"
    );

    /** 비속어 포함 여부 반환 (공백 제거 + 소문자 변환 후 부분 일치 검사) */
    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return BANNED_WORDS.stream().anyMatch(normalized::contains);
    }
}

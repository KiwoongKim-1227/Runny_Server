package com.goodspace.runny.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 에러코드 정의. 네이밍 규칙은 {도메인}_{3자리 번호} 형식이며 도메인별로 자유롭게 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 (COMMON)
    COMMON_001(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
    COMMON_002(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),
    COMMON_003(HttpStatus.BAD_REQUEST, "COMMON_003", "요청 본문을 읽을 수 없습니다."),

    // 인증/인가 (AUTH)
    AUTH_001(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    AUTH_002(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    AUTH_003(HttpStatus.UNAUTHORIZED, "AUTH_003", "유효하지 않은 토큰입니다."),
    AUTH_004(HttpStatus.UNAUTHORIZED, "AUTH_004", "만료된 토큰입니다."),

    // 외부 연동 (EXTERNAL)
    EXTERNAL_001(HttpStatus.BAD_GATEWAY, "EXTERNAL_001", "외부 API 호출에 실패했습니다."),

    // 이미지/파일 (IMAGE)
    IMAGE_001(HttpStatus.BAD_REQUEST, "IMAGE_001", "허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, webp)"),
    IMAGE_002(HttpStatus.BAD_REQUEST, "IMAGE_002", "이미지 크기는 10MB를 초과할 수 없습니다."),
    IMAGE_003(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_003", "이미지 업로드에 실패했습니다."),

    // 회원 (USER) - 도메인 단계에서 계속 추가
    USER_001(HttpStatus.CONFLICT, "USER_001", "이미 가입된 이메일입니다."),
    USER_002(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 닉네임입니다."),

    // 재화 (COIN)
    COIN_001(HttpStatus.BAD_REQUEST, "COIN_001", "코인 잔액이 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

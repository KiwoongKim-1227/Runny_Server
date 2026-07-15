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
    AUTH_005(HttpStatus.BAD_REQUEST, "AUTH_005", "인증코드가 일치하지 않습니다."),
    AUTH_006(HttpStatus.BAD_REQUEST, "AUTH_006", "인증코드가 만료되었습니다."),
    AUTH_007(HttpStatus.BAD_REQUEST, "AUTH_007", "이메일 인증이 완료되지 않았습니다."),
    AUTH_008(HttpStatus.BAD_REQUEST, "AUTH_008", "비밀번호 규칙을 충족하지 않습니다. (8자 이상, 영문/숫자/특수문자 포함)"),
    AUTH_009(HttpStatus.BAD_REQUEST, "AUTH_009", "필수 약관에 모두 동의해야 합니다."),
    AUTH_010(HttpStatus.UNAUTHORIZED, "AUTH_010", "소셜 토큰 검증에 실패했습니다."),
    AUTH_011(HttpStatus.UNAUTHORIZED, "AUTH_011", "유효하지 않은 refresh 토큰입니다."),
    AUTH_012(HttpStatus.UNAUTHORIZED, "AUTH_012", "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_013(HttpStatus.BAD_REQUEST, "AUTH_013", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    AUTH_014(HttpStatus.BAD_REQUEST, "AUTH_014", "유효하지 않거나 만료된 재설정 토큰입니다."),

    // 외부 연동 (EXTERNAL)
    EXTERNAL_001(HttpStatus.BAD_GATEWAY, "EXTERNAL_001", "외부 API 호출에 실패했습니다."),

    // 이미지/파일 (IMAGE)
    IMAGE_001(HttpStatus.BAD_REQUEST, "IMAGE_001", "허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, webp)"),
    IMAGE_002(HttpStatus.BAD_REQUEST, "IMAGE_002", "이미지 크기는 10MB를 초과할 수 없습니다."),
    IMAGE_003(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_003", "이미지 업로드에 실패했습니다."),

    // 회원 (USER) - 도메인 단계에서 계속 추가
    USER_001(HttpStatus.CONFLICT, "USER_001", "이미 가입된 이메일입니다."),
    USER_002(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 닉네임입니다."),
    USER_003(HttpStatus.NOT_FOUND, "USER_003", "존재하지 않는 유저입니다."),
    USER_004(HttpStatus.BAD_REQUEST, "USER_004", "이메일 형식이 올바르지 않습니다."),
    USER_005(HttpStatus.BAD_REQUEST, "USER_005", "닉네임 규칙을 충족하지 않습니다. (2~7자, 한글/영문/숫자만)"),
    USER_006(HttpStatus.BAD_REQUEST, "USER_006", "사용할 수 없는 단어가 포함되어 있습니다."),
    USER_007(HttpStatus.CONFLICT, "USER_007", "이미 소셜 계정으로 가입된 이메일입니다."),
    USER_008(HttpStatus.BAD_REQUEST, "USER_008", "소셜 로그인 유저는 비밀번호를 변경/찾기할 수 없습니다."),
    USER_009(HttpStatus.BAD_REQUEST, "USER_009", "현재 비밀번호가 일치하지 않습니다."),

    // 재화 (COIN)
    COIN_001(HttpStatus.BAD_REQUEST, "COIN_001", "코인 잔액이 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

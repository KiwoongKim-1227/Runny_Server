package com.goodspace.runny.global.exception;

import lombok.Getter;

/**
 * 도메인 비즈니스 규칙 위반 시 던지는 공통 예외. ErrorCode를 담아 전역 핸들러가 일관 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

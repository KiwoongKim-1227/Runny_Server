package com.goodspace.runny.global.response;

import com.goodspace.runny.global.exception.ErrorCode;

/**
 * 모든 API의 공통 응답 래퍼. { success, data, error{code, message} } 형태를 유지한다.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error
) {

    /** 에러 상세 정보 (코드 + 메시지) */
    public record ErrorBody(String code, String message) {
    }

    /** 성공 응답 생성 (데이터 포함) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 성공 응답 생성 (데이터 없음) */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    /** 실패 응답 생성 (ErrorCode 기반) */
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.getCode(), errorCode.getMessage()));
    }

    /** 실패 응답 생성 (메시지 오버라이드 - 검증 실패 등 동적 메시지용) */
    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.getCode(), message));
    }
}

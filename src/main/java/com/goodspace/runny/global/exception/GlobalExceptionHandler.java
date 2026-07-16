package com.goodspace.runny.global.exception;

import com.goodspace.runny.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리기. 모든 예외를 ApiResponse 실패 형태로 일관 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 외부 API 호출 실패 (502) - BusinessException보다 먼저 매칭되도록 별도 처리 */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApi(ExternalApiException e) {
        log.error("외부 API 호출 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    /** 도메인 비즈니스 예외 - ErrorCode에 정의된 상태/코드/메시지로 응답 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("비즈니스 예외: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage()));
    }

    /** @Valid 검증 실패 - 필드별 메시지를 조합해 400 응답 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + fieldMessage(f))
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.COMMON_001, message));
    }

    /** 요청 본문 파싱 실패 (JSON 형식 오류 등) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.COMMON_003));
    }

    /** 인증 실패 (401) */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ErrorCode.AUTH_001));
    }

    /** 인가 실패 (403) */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail(ErrorCode.AUTH_002));
    }

    /** 잘못된 인자 (enum 변환 실패, 도메인 검증용 IllegalArgument 등) - 500 대신 400으로 응답 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청 인자: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCode.COMMON_001));
    }

    /** 그 외 모든 예외 - 로그 기록 후 일반 메시지로 500 응답 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.COMMON_002));
    }

    /** 필드 에러 메시지 추출 (기본 메시지가 없으면 대체 문구 사용) */
    private String fieldMessage(FieldError fieldError) {
        String msg = fieldError.getDefaultMessage();
        return (msg == null || msg.isBlank()) ? "잘못된 값입니다." : msg;
    }
}

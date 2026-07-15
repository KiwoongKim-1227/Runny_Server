package com.goodspace.runny.domain.auth.service;

import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;

import java.util.regex.Pattern;

/**
 * 비밀번호 규칙 검증 유틸. 8자 이상 + 영문/숫자/특수문자 각 1자 이상 포함(서버 재검증).
 * 규칙 강도는 정규식 상수 교체로 조정 가능하다.
 */
public final class PasswordValidator {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_=+\\[\\]{};:'\",.<>/?\\\\|`~]).{8,}$");

    private PasswordValidator() {
    }

    /** 규칙 미충족 시 AUTH_008, 비밀번호/확인 불일치 시 AUTH_013 */
    public static void validate(String password, String passwordConfirm) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.AUTH_008);
        }
        if (!password.equals(passwordConfirm)) {
            throw new BusinessException(ErrorCode.AUTH_013);
        }
    }
}

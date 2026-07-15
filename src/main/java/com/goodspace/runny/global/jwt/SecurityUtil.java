package com.goodspace.runny.global.jwt;

import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext에서 현재 로그인한 유저 ID를 꺼내는 정적 유틸.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** 현재 인증된 유저 ID 반환. 인증 정보가 없으면 AUTH_001 예외 */
    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ErrorCode.AUTH_001);
        }
        return userId;
    }
}

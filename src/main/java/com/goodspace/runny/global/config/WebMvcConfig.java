package com.goodspace.runny.global.config;

import com.goodspace.runny.global.jwt.OnboardingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정. 온보딩 미완료 유저의 메인 API 접근을 차단하는 인터셉터를 등록한다.
 * 인증/온보딩 관련 경로와 Swagger는 제외한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final OnboardingInterceptor onboardingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(onboardingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 인증 관련 (로그인/가입/토큰/비밀번호)
                        "/api/auth/**",
                        // 온보딩 진행 API (프로필 저장, 닉네임 중복확인, 견종 목록, 강아지 생성)
                        "/api/users/nickname/check",
                        "/api/users/me/profile",
                        "/api/breeds",
                        "/api/dogs",
                        // 헬스체크
                        "/api/health",
                        // Swagger
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}

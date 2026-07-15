package com.goodspace.runny.global.jwt;

import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 7 설정. 무상태 JWT API 기준으로 세션/CSRF 비활성, 화이트리스트 외 전부 인증 필요.
 * 경로 매칭은 requestMatchers 문자열 패턴 사용 (AntPathRequestMatcher는 Security 7에서 제거됨).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // 인증 없이 접근 가능한 경로 (온보딩/인증 관련)
    private static final String[] WHITELIST = {
            "/api/auth/**",
            "/api/health",
            // Swagger UI / OpenAPI 문서 (운영 배포 시 차단 또는 인증 적용 권장)
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        // 인증 실패(401) / 인가 실패(403)를 ApiResponse JSON으로 반환
                        .authenticationEntryPoint((request, response, e) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTH_001))
                        .accessDeniedHandler((request, response, e) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.AUTH_002)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 비밀번호 해시용 BCrypt 인코더 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 필터 단계 에러를 공통 응답 형태(JSON)로 직렬화 - Jackson 3 JsonMapper 사용 */
    private void writeErrorResponse(HttpServletResponse response, int status, ErrorCode errorCode)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.fail(errorCode)));
    }
}

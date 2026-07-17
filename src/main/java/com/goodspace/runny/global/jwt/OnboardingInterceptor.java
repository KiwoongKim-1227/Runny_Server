package com.goodspace.runny.global.jwt;

import com.goodspace.runny.domain.user.entity.OnboardingStatus;
import com.goodspace.runny.domain.user.entity.User;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 온보딩 미완료 유저의 메인 API 접근을 차단하는 인터셉터.
 * onboardingStatus가 COMPLETED가 아닌 유저는 화이트리스트(인증/온보딩) 외 API 호출 시
 * 403 + 현재 온보딩 단계 정보를 반환한다. 프론트는 이 값으로 이어서 진행할 화면을 결정한다.
 */
@Component
@RequiredArgsConstructor
public class OnboardingInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 인증되지 않은 요청은 Security에서 처리하므로 통과
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return true;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return true;
        }

        if (user.getOnboardingStatus() == OnboardingStatus.COMPLETED) {
            return true;
        }

        // 온보딩 미완료 - 403 + 온보딩 단계 정보 반환
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> data = Map.of(
                "onboardingStatus", user.getOnboardingStatus().name(),
                "message", "온보딩을 완료해야 이용할 수 있습니다."
        );
        ApiResponse<Map<String, Object>> body = ApiResponse.ok(data);
        // success=false로 내려주기 위해 fail 형태 사용
        response.getWriter().write(jsonMapper.writeValueAsString(
                Map.of("success", false,
                        "data", data,
                        "error", Map.of("code", "ONBOARDING_REQUIRED",
                                "message", "온보딩을 완료해야 이용할 수 있습니다."))));
        return false;
    }
}

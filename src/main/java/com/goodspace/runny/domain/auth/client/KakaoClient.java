package com.goodspace.runny.domain.auth.client;

import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 카카오 사용자 정보 조회 클라이언트. SDK가 발급한 access token으로 /v2/user/me를 호출해
 * 회원번호(providerId)와 이메일을 확보한다. 1단계 공통 RestClient 빈을 재사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoClient {

    private static final String USER_ME_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** access token 검증 겸 사용자 정보 조회. 실패 시 AUTH_010 */
    public SocialUserInfo verify(String accessToken) {
        try {
            String body = restClient.get()
                    .uri(USER_ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode root = jsonMapper.readTree(body);
            String providerId = root.path("id").asString();
            if (providerId == null || providerId.isBlank()) {
                throw new BusinessException(ErrorCode.AUTH_010);
            }
            String email = root.path("kakao_account").path("email").asString(null);
            return new SocialUserInfo(Provider.KAKAO, providerId, email);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("카카오 토큰 검증 실패", e);
            throw new BusinessException(ErrorCode.AUTH_010);
        }
    }
}

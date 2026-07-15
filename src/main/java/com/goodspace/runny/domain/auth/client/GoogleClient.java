package com.goodspace.runny.domain.auth.client;

import com.goodspace.runny.domain.user.entity.Provider;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 구글 idToken 검증 클라이언트. tokeninfo 엔드포인트로 검증하고 aud가 우리 클라이언트 ID인지 확인한다.
 * 1단계 공통 RestClient 빈을 재사용한다.
 */
@Slf4j
@Component
public class GoogleClient {

    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    private final RestClient restClient;
    private final String clientId;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public GoogleClient(RestClient restClient,
                        @Value("${oauth.google.client-id}") String clientId) {
        this.restClient = restClient;
        this.clientId = clientId;
    }

    /** idToken 검증 후 sub/email 추출. aud 불일치 또는 호출 실패 시 AUTH_010 */
    public SocialUserInfo verify(String idToken) {
        try {
            String body = restClient.get()
                    .uri(TOKEN_INFO_URL, idToken)
                    .retrieve()
                    .body(String.class);
            JsonNode root = jsonMapper.readTree(body);
            String aud = root.path("aud").asString(null);
            String sub = root.path("sub").asString(null);
            if (sub == null || !clientId.equals(aud)) {
                throw new BusinessException(ErrorCode.AUTH_010);
            }
            String email = root.path("email").asString(null);
            return new SocialUserInfo(Provider.GOOGLE, sub, email);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("구글 idToken 검증 실패", e);
            throw new BusinessException(ErrorCode.AUTH_010);
        }
    }
}

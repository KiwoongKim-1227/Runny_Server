package com.goodspace.runny.domain.payment.client;

import com.goodspace.runny.global.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * NicePay 결제 승인/취소 클라이언트. 1단계 공통 RestClient 빈을 재사용하며 POST만 사용한다.
 * 인증은 Basic Auth(clientKey:secretKey), 엔드포인트는 yml의 nicepay.api-url 기준(샌드박스/운영 전환).
 */
@Slf4j
@Component
public class NicePayClient {

    private final RestClient restClient;
    private final String apiUrl;
    private final String basicAuthHeader;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public NicePayClient(RestClient restClient,
                         @Value("${nicepay.api-url}") String apiUrl,
                         @Value("${nicepay.client-key}") String clientKey,
                         @Value("${nicepay.secret-key}") String secretKey) {
        this.restClient = restClient;
        this.apiUrl = apiUrl;
        this.basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((clientKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
    }

    /** 승인 결과 - resultCode 0000이 성공, amount로 금액 위변조를 검증한다 */
    public record ApproveResult(boolean success, String resultCode, String resultMsg, int amount, String orderId) {
    }

    /** 결제 승인 API 호출 - POST /v1/payments/{tid}, body에 주문 금액 포함 */
    public ApproveResult approve(String tid, int amount) {
        try {
            String body = restClient.post()
                    .uri(apiUrl + "/v1/payments/" + tid)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amount))
                    .retrieve()
                    .body(String.class);
            JsonNode root = jsonMapper.readTree(body);
            String resultCode = root.path("resultCode").asString("");
            return new ApproveResult(
                    "0000".equals(resultCode),
                    resultCode,
                    root.path("resultMsg").asString(""),
                    root.path("amount").asInt(-1),
                    root.path("orderId").asString(""));
        } catch (Exception e) {
            log.error("NicePay 승인 API 호출 실패: tid={}", tid, e);
            throw new ExternalApiException("NicePay 승인 호출에 실패했습니다.");
        }
    }

    /** 결제 취소 API 호출 - POST /v1/payments/{tid}/cancel. 실패해도 상위 흐름은 계속(로그만 기록) */
    public void cancel(String tid, String orderId, String reason) {
        try {
            restClient.post()
                    .uri(apiUrl + "/v1/payments/" + tid + "/cancel")
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("reason", reason, "orderId", orderId))
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            // 취소 실패 건은 수동 대사 대상 - 결제 상태는 FAILED로 기록되므로 코인은 지급되지 않는다
            log.error("NicePay 취소 API 호출 실패(수동 확인 필요): tid={}, orderId={}", tid, orderId, e);
        }
    }

    // TODO(MVP 범위 외 확장 지점): Webhook 수신 엔드포인트 - NicePay 서버 통지로 승인/취소 상태 동기화
    // TODO(MVP 범위 외 확장 지점): 사용자 요청 결제 취소 / 부분 환불 API - /v1/payments/{tid}/cancel의 partialCancelCode 활용
}

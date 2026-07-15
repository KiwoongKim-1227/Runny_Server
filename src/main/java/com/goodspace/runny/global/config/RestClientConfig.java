package com.goodspace.runny.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 외부 API 호출 공통 RestClient 설정
 * Boot 4의 자동 구성 RestClient.Builder에 타임아웃을 적용해 공통 빈으로 제공합니다
 */
@Configuration
public class RestClientConfig {

    /**
     * connect 3초 / read 5초 타임아웃이 적용된 공통 RestClient 빈
     */
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        // Java 17 표준 환경에서 가장 안정적으로 동작하는 HTTP 클라이언트 팩토리입니다.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // 타임아웃 설정 (connect 3s / read 5s)
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        return builder
                .requestFactory(requestFactory)
                .build();
    }
}

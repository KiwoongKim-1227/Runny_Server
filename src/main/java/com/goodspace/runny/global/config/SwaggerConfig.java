package com.goodspace.runny.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정. JWT Bearer 인증 스킴을 전역 등록해
 * Swagger UI 우측 상단 Authorize 버튼으로 access 토큰을 넣고 테스트할 수 있다.
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /** API 기본 정보 + JWT Bearer 보안 스킴 정의 */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Runny API")
                        .description("강아지 러닝 서비스 Runny 백엔드 API 문서")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("access 토큰을 입력하세요. (Bearer 접두사 불필요)")))
                // 전역 보안 요구 - 화이트리스트 API도 UI상 자물쇠가 표시되지만 토큰 없이 호출 가능
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}

package com.goodspace.runny.global.health;

import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 서버 상태 확인용 헬스체크 컨트롤러. 인증 없이 접근 가능(화이트리스트).
 */
@Tag(name = "Health", description = "서버 상태 확인")
@RestController
public class HealthController {

    /** 서버 동작 여부와 서버 시간(Asia/Seoul) 반환 */
    @Operation(summary = "헬스체크", description = "서버 동작 여부와 서버 시간(Asia/Seoul) 반환")
    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "serverTime", LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString()
        ));
    }
}

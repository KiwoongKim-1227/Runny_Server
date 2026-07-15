package com.goodspace.runny.global.util;

import com.goodspace.runny.global.exception.ExternalApiException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 인증코드 메일 발송 유틸. Naver SMTP(JavaMailSender 자동 구성)를 사용하며
 * 가입/비밀번호 찾기 양쪽에서 재사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationMailSender {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${mail.verification-code-expiry-minutes}")
    private int expiryMinutes;

    /** 6자리 인증코드 메일 발송. 발송 실패는 외부 API 예외로 변환 */
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("[Runny] 이메일 인증코드 안내");
            helper.setText(buildBody(code), true);
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("인증코드 메일 발송 실패: to={}", to, e);
            throw new ExternalApiException("인증코드 메일 발송에 실패했습니다.");
        }
    }

    /** 메일 본문 HTML 생성 */
    private String buildBody(String code) {
        return """
                <div style="font-family: sans-serif; padding: 24px;">
                  <h2>Runny 이메일 인증</h2>
                  <p>아래 인증코드를 입력해 주세요. 유효 시간은 %d분입니다.</p>
                  <div style="font-size: 28px; font-weight: bold; letter-spacing: 6px;">%s</div>
                </div>
                """.formatted(expiryMinutes, code);
    }
}

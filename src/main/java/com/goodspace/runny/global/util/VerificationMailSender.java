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

    /** 메일 본문 HTML 생성 - Runny 앱 메인화면 색감(하늘/초록/코랄/브라운) 기반 디자인 */
    private String buildBody(String code) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#D8EEF7; font-family:'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#D8EEF7; padding: 40px 0;">
                    <tr>
                      <td align="center">
                        <table width="520" cellpadding="0" cellspacing="0" border="0" style="max-width:520px; width:100%%;">

                          <!-- 상단 하늘 배경 헤더 -->
                          <tr>
                            <td style="background: linear-gradient(180deg, #B8DDEF 0%%, #C8E8F5 60%%, #A8D5B0 100%%);
                                        border-radius: 20px 20px 0 0;
                                        padding: 36px 32px 28px;
                                        text-align: center;">
                              <!-- 발바닥 아이콘 -->
                              <div style="font-size: 42px; margin-bottom: 8px;">🐾</div>
                              <div style="font-size: 26px; font-weight: 800; color: #5C3D1E;
                                          letter-spacing: -0.5px; text-shadow: 0 1px 0 rgba(255,255,255,0.4);">
                                Runny
                              </div>
                              <div style="font-size: 13px; color: #6B7A5E; margin-top: 4px; font-weight: 500;">
                                강아지와 함께 달리는 즐거움
                              </div>
                            </td>
                          </tr>

                          <!-- 잔디 구분선 -->
                          <tr>
                            <td style="background: #7BC67A; height: 6px; font-size:0; line-height:0;">&nbsp;</td>
                          </tr>

                          <!-- 본문 카드 -->
                          <tr>
                            <td style="background-color: #FFFEF9; padding: 36px 36px 28px; border-radius: 0 0 20px 20px;
                                        box-shadow: 0 4px 16px rgba(0,0,0,0.08);">

                              <!-- 인사말 -->
                              <p style="margin: 0 0 8px; font-size: 18px; font-weight: 700; color: #5C3D1E;">
                                안녕하세요! 🐶
                              </p>
                              <p style="margin: 0 0 28px; font-size: 14px; color: #7A6A5A; line-height: 1.7;">
                                아래 인증코드를 입력해 주세요.<br>
                                인증코드는 <strong style="color:#E07B50;">%d분</strong> 동안 유효합니다.
                              </p>

                              <!-- 인증코드 박스 -->
                              <div style="background: linear-gradient(135deg, #FFF3EE 0%%, #FFE8DE 100%%);
                                          border: 2px solid #F4A07A;
                                          border-radius: 16px;
                                          padding: 24px 16px;
                                          text-align: center;
                                          margin-bottom: 28px;">
                                <div style="font-size: 11px; font-weight: 600; color: #B06040;
                                            letter-spacing: 2px; text-transform: uppercase; margin-bottom: 12px;">
                                  인증코드
                                </div>
                                <div style="font-size: 38px; font-weight: 800; color: #D45D2A;
                                            letter-spacing: 10px; font-variant-numeric: tabular-nums;">
                                  %s
                                </div>
                              </div>

                              <!-- 주의사항 -->
                              <div style="background-color: #F0F7F0; border-left: 4px solid #7BC67A;
                                          border-radius: 0 10px 10px 0; padding: 14px 16px; margin-bottom: 8px;">
                                <p style="margin: 0; font-size: 12px; color: #5A7A5A; line-height: 1.7;">
                                   본인이 요청하지 않은 경우, 이 메일을 무시해 주세요.<br>
                                   코드는 %d분 후 자동으로 만료됩니다.
                                </p>
                              </div>

                            </td>
                          </tr>

                          <!-- 하단 여백 -->
                          <tr>
                            <td style="padding: 20px 0; text-align: center;">
                              <p style="margin: 0; font-size: 11px; color: #8BA8B0;">
                                ⓒ Runny. All rights reserved.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(expiryMinutes, code, expiryMinutes);
    }
}

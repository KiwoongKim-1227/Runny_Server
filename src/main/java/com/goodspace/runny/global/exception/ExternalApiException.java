package com.goodspace.runny.global.exception;

/**
 * 외부 API(카카오/구글/애플/NicePay/S3 등) 호출 실패를 나타내는 예외. 전역 핸들러가 502로 변환한다.
 */
public class ExternalApiException extends BusinessException {

    public ExternalApiException(String message) {
        super(ErrorCode.EXTERNAL_001, message);
    }

    public ExternalApiException() {
        super(ErrorCode.EXTERNAL_001);
    }
}

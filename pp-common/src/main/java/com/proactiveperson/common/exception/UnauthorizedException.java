package com.proactiveperson.common.exception;

public class UnauthorizedException extends AppException {

    public UnauthorizedException() {
        super("UNAUTHORIZED", "未授权，请提供有效的 X-API-Key");
    }

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}

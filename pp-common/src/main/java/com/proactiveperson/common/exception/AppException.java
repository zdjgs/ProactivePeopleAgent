package com.proactiveperson.common.exception;

/**
 * 业务异常基类，由全局异常处理器映射为统一错误体。
 */
public class AppException extends RuntimeException {

    private final String code;

    public AppException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

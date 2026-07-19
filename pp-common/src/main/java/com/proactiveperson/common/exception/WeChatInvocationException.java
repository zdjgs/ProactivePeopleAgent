package com.proactiveperson.common.exception;

public class WeChatInvocationException extends AppException {

    public static final String CODE = "WECHAT_INVOCATION_FAILED";

    public WeChatInvocationException(String message) {
        super(CODE, message);
    }

    public WeChatInvocationException(String message, Throwable cause) {
        super(CODE, message);
        initCause(cause);
    }
}

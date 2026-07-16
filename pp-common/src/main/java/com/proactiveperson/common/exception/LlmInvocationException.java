package com.proactiveperson.common.exception;

public class LlmInvocationException extends AppException {

    public static final String CODE = "LLM_INVOCATION_FAILED";

    public LlmInvocationException(String message) {
        super(CODE, message);
    }

    public LlmInvocationException(String message, Throwable cause) {
        super(CODE, message);
        initCause(cause);
    }
}

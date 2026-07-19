package com.proactiveperson.common.exception;

public class MemoryInvocationException extends AppException {

    public static final String CODE = "MEMORY_INVOCATION_FAILED";

    public MemoryInvocationException(String message) {
        super(CODE, message);
    }

    public MemoryInvocationException(String message, Throwable cause) {
        super(CODE, message);
        initCause(cause);
    }
}

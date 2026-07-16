package com.proactiveperson.common.api;

public record ApiResponse<T>(boolean success, String message, T data, String code) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "ok", data, null);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return fail(message, null);
    }

    public static <T> ApiResponse<T> fail(String message, String code) {
        return new ApiResponse<>(false, message, null, code);
    }
}

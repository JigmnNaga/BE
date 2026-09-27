package com.jigmnnaga.global.response;

public record CommonResponse<T>(
        boolean success,
        String message,
        T data
) {

    public static <T> CommonResponse<T> success(ResponseMessage message, T data) {
        return new CommonResponse<>(true, message.getMessage(), data);
    }

    public static <T> CommonResponse<T> success(ResponseMessage message) {
        return new CommonResponse<>(true, message.getMessage(), null);
    }

    public static CommonResponse<Void> fail(String message) {
        return new CommonResponse<>(false, message, null);
    }
}

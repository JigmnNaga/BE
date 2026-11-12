package com.jigmnnaga.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jigmnnaga.application.exception.ErrorCode;

public record CommonResponse<T>(
        boolean success,
        @JsonInclude(JsonInclude.Include.NON_NULL) String code,
        String message,
        T data
) {

    public static <T> CommonResponse<T> success(ResponseMessage message, T data) {
        return new CommonResponse<>(true, null, message.getMessage(), data);
    }

    public static <T> CommonResponse<T> success(ResponseMessage message) {
        return new CommonResponse<>(true, null, message.getMessage(), null);
    }

    public static CommonResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static CommonResponse<Void> fail(ErrorCode errorCode, String message) {
        return new CommonResponse<>(false, errorCode.name(), message, null);
    }
}

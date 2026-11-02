package com.jigmnnaga.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseMessage {

    LOGIN_SUCCESS("로그인에 성공했습니다."),
    APPOINTMENT_CREATE_SUCCESS("약속방이 생성되었습니다.");

    private final String message;
}

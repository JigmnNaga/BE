package com.jigmnnaga.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseMessage {

    LOGIN_SUCCESS("로그인에 성공했습니다."),
    APPOINTMENT_CREATE_SUCCESS("약속방이 생성되었습니다."),
    INVITE_GET_SUCCESS("초대 정보를 조회했습니다."),
    INVITE_ACCEPT_SUCCESS("약속을 수락했습니다."),
    INVITE_DECLINE_SUCCESS("약속을 거절했습니다.");

    private final String message;
}

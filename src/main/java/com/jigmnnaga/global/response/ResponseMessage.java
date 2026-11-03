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
    INVITE_DECLINE_SUCCESS("약속을 거절했습니다."),
    SCHEDULE_SUBMIT_SUCCESS("일정 선택을 완료했습니다."),
    SCHEDULE_GET_SUCCESS("내 일정 선택을 조회했습니다."),
    APPOINTMENT_DETAIL_GET_SUCCESS("약속 상세 정보를 조회했습니다."),
    SCHEDULE_STATUS_GET_SUCCESS("일정 응답 현황을 조회했습니다."),
    CONFIRM_DATE_TIME_SUCCESS("약속 날짜와 시간을 확정했습니다."),
    CONFIRM_LOCATION_SUCCESS("약속 장소를 확정했습니다."),
    ATTENDANCE_RESPOND_SUCCESS("참석 응답을 저장했습니다."),
    MY_APPOINTMENTS_GET_SUCCESS("내 약속 목록을 조회했습니다."),
    NEXT_APPOINTMENT_GET_SUCCESS("다음 약속을 조회했습니다."),
    CALENDAR_GET_SUCCESS("월별 캘린더를 조회했습니다.");

    private final String message;
}

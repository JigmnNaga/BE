package com.jigmnnaga.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseMessage {

    LOGIN_SUCCESS("로그인에 성공했습니다."),
    TOKEN_REISSUE_SUCCESS("토큰을 재발급했습니다."),
    LOGOUT_SUCCESS("로그아웃했습니다."),
    PLACE_SEARCH_SUCCESS("장소를 검색했습니다."),
    TRANSIT_ROUTE_SUCCESS("대중교통 경로를 조회했습니다."),
    WITHDRAW_SUCCESS("회원 탈퇴가 완료되었습니다."),
    MY_INFO_GET_SUCCESS("내 정보를 조회했습니다."),
    PROFILE_UPDATE_SUCCESS("프로필을 수정했습니다."),
    PROFILE_IMAGE_DELETE_SUCCESS("프로필 이미지를 기본 이미지로 변경했습니다."),
    PROFILE_IMAGE_UPLOAD_URL_ISSUE_SUCCESS("프로필 이미지 업로드 URL을 발급했습니다."),
    DEFAULT_LOCATION_UPDATE_SUCCESS("기본 위치를 저장했습니다."),
    DEFAULT_LOCATION_DELETE_SUCCESS("기본 위치를 삭제했습니다."),
    APPOINTMENT_CREATE_SUCCESS("약속방이 생성되었습니다."),
    INVITE_GET_SUCCESS("초대 정보를 조회했습니다."),
    INVITE_ACCEPT_SUCCESS("약속을 수락했습니다."),
    INVITE_DECLINE_SUCCESS("약속을 거절했습니다."),
    SCHEDULE_SUBMIT_SUCCESS("일정 선택을 완료했습니다."),
    SCHEDULE_GET_SUCCESS("내 일정 선택을 조회했습니다."),
    APPOINTMENT_DETAIL_GET_SUCCESS("약속 상세 정보를 조회했습니다."),
    SCHEDULE_STATUS_GET_SUCCESS("일정 응답 현황을 조회했습니다."),
    SCHEDULE_HEATMAP_GET_SUCCESS("시간대별 가능 인원을 조회했습니다."),
    CHAT_MESSAGES_GET_SUCCESS("이전 메시지를 조회했습니다."),
    CHAT_READ_SUCCESS("메시지를 읽음 처리했습니다."),
    NOTIFICATIONS_GET_SUCCESS("알림 목록을 조회했습니다."),
    TRAVEL_OPTIONS_GET_SUCCESS("이동수단 후보를 조회했습니다."),
    TRAVEL_MODE_SELECT_SUCCESS("이동수단을 선택했습니다."),
    DEPARTURE_LOCATION_REGISTER_SUCCESS("출발 위치를 등록했습니다."),
    ESTIMATED_TRAVEL_TIME_GET_SUCCESS("등록된 출발 위치 기준 예상 이동시간을 조회했습니다."),
    NOTIFICATION_SETTING_UPDATE_SUCCESS("약속방 알림 설정을 변경했습니다."),
    DEVICE_TOKEN_REGISTER_SUCCESS("기기 토큰을 등록했습니다."),
    DEVICE_TOKEN_DELETE_SUCCESS("기기 토큰을 삭제했습니다."),
    NOTIFICATION_UNREAD_COUNT_GET_SUCCESS("안 읽은 알림 수를 조회했습니다."),
    NOTIFICATION_READ_SUCCESS("알림을 읽음 처리했습니다."),
    REMINDER_SEND_SUCCESS("미응답자에게 요청 알림을 보냈습니다."),
    CONFIRM_DATE_TIME_SUCCESS("약속 날짜와 시간을 확정했습니다."),
    CONFIRM_LOCATION_SUCCESS("약속 장소를 확정했습니다."),
    ATTENDANCE_RESPOND_SUCCESS("참석 응답을 저장했습니다."),
    MY_APPOINTMENTS_GET_SUCCESS("내 약속 목록을 조회했습니다."),
    NEXT_APPOINTMENT_GET_SUCCESS("다음 약속을 조회했습니다."),
    CALENDAR_GET_SUCCESS("월별 캘린더를 조회했습니다."),
    APPOINTMENT_UPDATE_SUCCESS("약속 정보를 변경했습니다."),
    APPOINTMENT_CANCEL_SUCCESS("약속을 취소했습니다."),
    APPOINTMENT_DELETE_SUCCESS("약속방을 삭제했습니다."),
    APPOINTMENT_LEAVE_SUCCESS("약속방을 나갔습니다."),
    PARTICIPANT_KICK_SUCCESS("멤버를 내보냈습니다."),
    HOST_DELEGATE_SUCCESS("방장을 위임했습니다."),
    MY_INVITES_GET_SUCCESS("받은 초대 목록을 조회했습니다.");

    private final String message;
}
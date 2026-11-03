package com.jigmnnaga.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    KAKAO_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 카카오 액세스 토큰입니다."),
    KAKAO_ACCOUNT_INFO_MISSING(HttpStatus.BAD_REQUEST, "카카오 동의 항목(닉네임)이 누락되어 로그인할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    DUPLICATE_CANDIDATE_DATE(HttpStatus.BAD_REQUEST, "후보 날짜가 중복되었습니다."),
    PAST_CANDIDATE_DATE(HttpStatus.BAD_REQUEST, "지난 날짜는 후보 날짜로 선택할 수 없습니다."),
    INVALID_CANDIDATE_DATES(HttpStatus.BAD_REQUEST, "날짜 선택 방식에 맞지 않는 후보 날짜입니다."),
    CANDIDATE_DATE_BEFORE_RESPONSE_DEADLINE(HttpStatus.BAD_REQUEST, "후보 날짜는 응답 기한 이후여야 합니다. 응답 기한을 먼저 변경해주세요."),
    INVALID_CANDIDATE_LOCATION_COUNT(HttpStatus.BAD_REQUEST, "후보 장소는 1개 이상 5개 이하여야 합니다."),
    INVALID_CANDIDATE_LOCATION(HttpStatus.BAD_REQUEST, "약속의 후보 장소가 아닙니다."),
    INVALID_RESPONSE_DEADLINE(HttpStatus.BAD_REQUEST, "응답 기한은 현재 이후이면서 첫 후보 날짜 이전이어야 합니다."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다."),

    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 초대 링크입니다."),
    INVITE_EXPIRED(HttpStatus.BAD_REQUEST, "응답 기한이 지나 만료된 초대 링크입니다."),
    HOST_CANNOT_RESPOND(HttpStatus.BAD_REQUEST, "방장은 초대에 응답할 수 없습니다."),
    ALREADY_RESPONDED(HttpStatus.CONFLICT, "이미 초대에 응답했습니다."),

    NOT_APPOINTMENT_PARTICIPANT(HttpStatus.FORBIDDEN, "약속에 참여한 멤버만 이용할 수 있습니다."),
    APPOINTMENT_CANCELED(HttpStatus.BAD_REQUEST, "취소된 약속입니다."),
    NOT_APPOINTMENT_HOST(HttpStatus.FORBIDDEN, "방장만 이용할 수 있습니다."),
    INVALID_CONFIRMED_DATE(HttpStatus.BAD_REQUEST, "후보 날짜에 포함되지 않은 날짜는 확정할 수 없습니다."),
    APPOINTMENT_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "약속 일정과 장소가 확정된 후에 참석 응답할 수 있습니다."),
    APPOINTMENT_TIME_PASSED(HttpStatus.BAD_REQUEST, "약속 시간이 지나 참석 응답을 변경할 수 없습니다."),
    INVALID_ATTENDANCE_STATUS(HttpStatus.BAD_REQUEST, "참석 또는 불참으로만 응답할 수 있습니다."),
    INVALID_CONFIRMED_LOCATION(HttpStatus.BAD_REQUEST, "약속의 후보 장소가 아닌 장소는 확정할 수 없습니다."),
    INVALID_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "후보 날짜에 포함되지 않은 날짜입니다."),
    DUPLICATE_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "같은 날짜를 중복해서 선택할 수 없습니다."),
    INVALID_AVAILABLE_TIME(HttpStatus.BAD_REQUEST, "시작 시간은 종료 시간보다 빨라야 합니다."),
    INVALID_PREFERRED_LOCATION(HttpStatus.BAD_REQUEST, "약속의 후보 장소가 아닙니다.");

    private final HttpStatus httpStatus;
    private final String message;
}

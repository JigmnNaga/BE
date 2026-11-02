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
    INVALID_RESPONSE_DEADLINE(HttpStatus.BAD_REQUEST, "응답 기한은 현재 이후이면서 첫 후보 날짜 이전이어야 합니다."),
    INVITE_CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "초대 코드 생성에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}

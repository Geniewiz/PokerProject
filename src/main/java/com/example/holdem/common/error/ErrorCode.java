package com.example.holdem.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."),
    TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "TABLE_404", "테이블을 찾을 수 없습니다."),
    HAND_NOT_FOUND(HttpStatus.NOT_FOUND, "GAME_404", "진행 중인 핸드를 찾을 수 없습니다."),
    INSUFFICIENT_PLAYERS(HttpStatus.BAD_REQUEST, "TABLE_400", "핸드를 시작하기 위한 플레이어 수가 부족합니다."),
    SEAT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "TABLE_400", "선택한 좌석을 사용할 수 없습니다."),
    CHIP_STACK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "CHIP_400", "칩이 부족합니다."),
    INVALID_ACTION(HttpStatus.BAD_REQUEST, "GAME_400", "현재 상태에서 허용되지 않는 액션입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}

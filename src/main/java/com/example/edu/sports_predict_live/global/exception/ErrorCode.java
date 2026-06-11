package com.example.edu.sports_predict_live.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 이메일 인증
    VERIFY_NOT_FOUND(404, "인증 요청 정보가 없습니다."),
    VERIFY_EXPIRED(400, "인증코드가 만료되었습니다."),
    VERIFY_CODE_MISMATCH(400, "인증코드가 일치하지 않습니다."),
    VERIFY_MAX_ATTEMPT(400, "인증 시도 횟수를 초과했습니다."),
    EMAIL_NOT_VERIFIED(400, "이메일 인증이 완료되지 않았습니다."),

    // 회원
    SAME_PASSWORD(400, "현재 비밀번호와 동일합니다."),
    ALREADY_DELETED(400, "이미 탈퇴한 회원입니다."),

    DUPLICATE_LOGIN_ID(409, "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(409, "이미 사용 중인 닉네임입니다."),

    USER_NOT_FOUND(404, "존재하지 않는 회원입니다."),
    EMAIL_NOT_FOUND(404, "가입된 이메일이 없습니다."),

    INVALID_PASSWORD(401, "비밀번호가 일치하지 않습니다."),

    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),

    // 팀
    TEAM_NOT_FOUND(404, "존재하지 않는 팀입니다."),
    ALREADY_FAVORITE(409, "이미 관심 팀으로 등록되어 있습니다."),
    FAVORITE_NOT_FOUND(404, "등록되지 않은 관심 팀입니다."),

    // 선수
    PLAYER_NOT_FOUND(404, "존재하지 않는 선수입니다.");

    private final int status;
    private final String message;
}
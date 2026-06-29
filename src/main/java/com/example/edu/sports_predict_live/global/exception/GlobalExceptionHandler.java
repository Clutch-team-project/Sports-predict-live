package com.example.edu.sports_predict_live.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// 공통 예외 처리 — {status, message} JSON 응답으로 변환
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustom(CustomException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(Map.of(
                        "status", e.getErrorCode().getStatus(),
                        "message", e.getErrorCode().getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "message", message));
    }
    // 비즈니스 검증 및 인자 예외 발생 시 메시지 반환
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleIllegalException(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // Spring Security/HTTP 상태 예외는 프레임워크가 처리하도록 재던지기 (401/403/4xx 마스킹 방지)
    @ExceptionHandler({
            AuthenticationException.class,
            AccessDeniedException.class,
            ResponseStatusException.class,
            ErrorResponseException.class
    })
    public void rethrowFrameworkException(Exception e) throws Exception {
        throw e;
    }

    // 예상치 못한 서버 오류 — 로그에는 스택트레이스, 응답에는 일반 메시지만
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "status", 500,
                        "message", "서버 오류가 발생했습니다.",
                        "devMessage", e.getClass().getSimpleName() + ": " + e.getMessage()
                ));
    }
}
package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.request.*;
import com.example.edu.sports_predict_live.user.dto.response.ReissueResponseDTO;
import com.example.edu.sports_predict_live.user.dto.response.TokenResponseDTO;
import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.service.EmailVerifyService;
import com.example.edu.sports_predict_live.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 인증 API — 회원가입, 이메일 인증, 로그인/로그아웃, 토큰 재발급, 아이디/비밀번호 찾기
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailVerifyService emailVerifyService;

    @PostMapping("/email/send")
    public ResponseEntity<Map<String, String>> sendCode(
            @Valid @RequestBody EmailSendRequestDTO dto) {
        emailVerifyService.sendCode(dto.getEmail(), dto.getPurpose());
        return ResponseEntity.ok(Map.of("message", "인증코드 발송 완료"));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Map<String, String>> verifyCode(
            @Valid @RequestBody EmailVerifyCheckDTO dto) {
        emailVerifyService.verifyCode(dto.getEmail(), dto.getCode(), dto.getPurpose());
        return ResponseEntity.ok(Map.of("message", "인증 완료"));
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(
            @Valid @RequestBody SignupRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.signup(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT 방식 — 클라이언트에서 토큰 삭제
        // 추후 Refresh Token 블랙리스트 적용 가능
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponseDTO> reissue(
            @RequestHeader("Authorization") String bearerToken) {
        String refreshToken = bearerToken.replace("Bearer ", "");
        return ResponseEntity.ok(userService.reissue(refreshToken));
    }

    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                userService.findLoginId(body.get("email"), body.get("code")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto) {
        userService.resetPassword(dto);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 재설정되었습니다."));
    }
}
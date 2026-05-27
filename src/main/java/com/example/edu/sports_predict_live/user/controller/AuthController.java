package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.request.EmailSendRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.EmailVerifyCheckDTO;
import com.example.edu.sports_predict_live.user.dto.request.LoginRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.SignupRequestDTO;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailVerifyService emailVerifyService;

    // 이메일 인증코드 발송
    @PostMapping("/email/send")
    public ResponseEntity<Map<String, String>> sendCode(
            @Valid @RequestBody EmailSendRequestDTO dto) {
        emailVerifyService.sendCode(dto.getEmail(), dto.getPurpose());
        return ResponseEntity.ok(Map.of("message", "인증코드 발송 완료"));
    }

    // 이메일 인증코드 확인
    @PostMapping("/email/verify")
    public ResponseEntity<Map<String, String>> verifyCode(
            @Valid @RequestBody EmailVerifyCheckDTO dto) {
        emailVerifyService.verifyCode(dto.getEmail(), dto.getCode(), dto.getPurpose());
        return ResponseEntity.ok(Map.of("message", "인증 완료"));
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDTO> signup(
            @Valid @RequestBody SignupRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.signup(dto));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(userService.login(dto));
    }
}
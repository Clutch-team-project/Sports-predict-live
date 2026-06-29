package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.request.ChangePasswordRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.DeleteUserRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.ResetPasswordRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.UserUpdateDTO;
import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 내 정보 API — 조회/수정/비밀번호 변경/회원 탈퇴
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getMe(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PutMapping(value = "/me", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<UserResponseDTO> updateMe(
            @AuthenticationPrincipal Long userId,
            @ModelAttribute UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateMe(userId, dto));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteMe(
            @AuthenticationPrincipal Long userId,
            @RequestBody DeleteUserRequestDTO dto) {
        userService.deleteMe(userId, dto);
        return ResponseEntity.ok(Map.of("message", "탈퇴가 완료되었습니다."));
    }

    @GetMapping("/findId")
    public ResponseEntity<Map<String, String>> findId(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                userService.findLoginId(body.get("email"), body.get("code")));
    }

    @GetMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto) {
        userService.resetPassword(dto);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 재설정되었습니다."));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChangePasswordRequestDTO dto) {
        userService.changePassword(userId, dto);
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
    }
}
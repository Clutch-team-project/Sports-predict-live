package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.request.ChangePasswordRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.ResetPasswordRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.UserUpdateDTO;
import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateMe(userId, dto));
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
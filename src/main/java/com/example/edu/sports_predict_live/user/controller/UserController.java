package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
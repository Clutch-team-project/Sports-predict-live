package com.example.edu.sports_predict_live.user.dto.response;

import com.example.edu.sports_predict_live.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDTO {

    private Long userId;
    private String loginId;
    private String email;
    private String name;
    private String nickname;
    private LocalDateTime createdAt;

    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
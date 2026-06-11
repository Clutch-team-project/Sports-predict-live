package com.example.edu.sports_predict_live.user.dto.response;

import com.example.edu.sports_predict_live.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDTO {

    private Long userId;
    private String loginId;
    private String email;
    private String name;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;

    private String phone;
    private LocalDate birthDate;
    private boolean marketingAgreed;
    private boolean matchStartAlert;
    private boolean predictionResultAlert;
    private int alertBeforeMinutes;

    public static UserResponseDTO from(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .marketingAgreed(user.isMarketingAgreed())
                .matchStartAlert(user.isMatchStartAlert())
                .predictionResultAlert(user.isPredictionResultAlert())
                .alertBeforeMinutes(user.getAlertBeforeMinutes())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
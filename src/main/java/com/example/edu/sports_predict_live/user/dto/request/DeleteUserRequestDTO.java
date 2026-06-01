package com.example.edu.sports_predict_live.user.dto.request;

import lombok.Getter;

@Getter
public class DeleteUserRequestDTO {
    private String password;  // 소셜 전용 계정은 null 허용
}
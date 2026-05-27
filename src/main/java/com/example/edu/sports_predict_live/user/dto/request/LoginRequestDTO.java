package com.example.edu.sports_predict_live.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDTO {

    @NotBlank
    private String loginId;
    @NotBlank
    private String password;
}
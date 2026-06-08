package com.example.edu.sports_predict_live.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ResetPasswordRequestDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    private String newPassword;

}
package com.example.edu.sports_predict_live.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

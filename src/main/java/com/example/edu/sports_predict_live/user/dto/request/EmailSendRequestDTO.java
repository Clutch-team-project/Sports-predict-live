package com.example.edu.sports_predict_live.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EmailSendRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String purpose;
}
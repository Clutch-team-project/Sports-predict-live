package com.example.edu.sports_predict_live.user.dto.request;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UserUpdateDTO {
    private String nickname;
    private String phone;
    private LocalDate birthDate;
    private String profileImage;
    private Boolean marketingAgreed;
    private Boolean matchStartAlert;
    private Boolean predictionResultAlert;
    private Integer alertBeforeMinutes;
}
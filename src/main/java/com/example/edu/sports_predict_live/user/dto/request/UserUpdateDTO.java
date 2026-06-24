package com.example.edu.sports_predict_live.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class UserUpdateDTO {
    private String nickname;
    private String phone;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;
    private MultipartFile profileImage;
    private Boolean marketingAgreed;
    private Boolean matchStartAlert;
    private Boolean predictionResultAlert;
    private Integer alertBeforeMinutes;
}
package com.example.edu.sports_predict_live.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class SignupRequestDTO {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$",
            message = "아이디는 영문/숫자 4~20자입니다.")
    private String loginId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 영문+숫자 포함 8~20자입니다.")
    private String password;

    @NotBlank
    @Size(max = 20)
    private String name;

    @Size(max = 20)
    private String nickname;

    private String phone;

    private LocalDate birthDate;

    private boolean marketingAgreed = false;

    private boolean matchStartAlert = false;

    private boolean predictionResultAlert = false;
}
package com.example.edu.sports_predict_live.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AdminCreateUserRequestDTO {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문·숫자 4~20자입니다.")
    private String loginId;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 영문+숫자 포함 8~20자입니다.")
    private String password;

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotBlank
    @Email
    private String email;

    @Size(max = 20)
    private String nickname;
}

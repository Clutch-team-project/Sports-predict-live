package com.example.edu.sports_predict_live.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponseDTO {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String nickname;
    private String loginId;
}
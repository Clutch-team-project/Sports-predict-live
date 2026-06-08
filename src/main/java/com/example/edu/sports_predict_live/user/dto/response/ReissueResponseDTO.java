package com.example.edu.sports_predict_live.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReissueResponseDTO {
    private String accessToken;
    private String refreshToken;
}
package com.example.edu.sports_predict_live.aiprediction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PredictionRankingDto {

    // TODO
    // users.nickname 연동 후 추가 예정
    // private String nickname;

    private Long userId;

    private Long totalPredictions;

    private Long correctPredictions;

    private Double accuracy;
}
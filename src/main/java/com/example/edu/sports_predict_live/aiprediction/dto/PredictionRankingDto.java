package com.example.edu.sports_predict_live.aiprediction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PredictionRankingDto {

    private Long userId;

    // 총 예측 수
    private Long totalPredictions;

    // 맞춘 수
    private Long correctPredictions;

    // 적중률
    private Double accuracy;

}
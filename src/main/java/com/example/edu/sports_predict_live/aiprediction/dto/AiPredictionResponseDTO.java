package com.example.edu.sports_predict_live.aiprediction.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiPredictionResponseDTO {

    private double homeWinProb;   // 백분율 (e.g. 58.7)
    private double drawProb;
    private double awayWinProb;   // 백분율 (e.g. 41.3)

    private String reasoning;           // 예측 근거 설명 (2~3문장)
    private List<String> keyFactors;    // 핵심 근거 키워드 목록

    private String predictedResult;     // "HOME_WIN" | "AWAY_WIN" | "DRAW"
}

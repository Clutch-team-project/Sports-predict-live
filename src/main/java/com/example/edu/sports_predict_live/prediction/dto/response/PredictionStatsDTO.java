package com.example.edu.sports_predict_live.prediction.dto.response;

import lombok.Getter;

@Getter
public class PredictionStatsDTO {

    private final long totalCount;
    private final double accuracyRate; // 0.0 ~ 100.0, 소수점 1자리

    public PredictionStatsDTO(long totalCount, long correctCount) {
        this.totalCount = totalCount;
        this.accuracyRate = totalCount > 0
                ? Math.round(correctCount * 1000.0 / totalCount) / 10.0
                : 0.0;
    }

    // AI 통계용: 표시 건수(total)와 적중률 분모(settled)를 분리
    public PredictionStatsDTO(long totalCount, long settledCount, long correctCount) {
        this.totalCount = totalCount;
        this.accuracyRate = settledCount > 0
                ? Math.round(correctCount * 1000.0 / settledCount) / 10.0
                : 0.0;
    }
}

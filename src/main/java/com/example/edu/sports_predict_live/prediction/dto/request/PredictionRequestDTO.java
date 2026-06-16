package com.example.edu.sports_predict_live.prediction.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PredictionRequestDTO {

    private Long matchId;              // KBO·K리그 DB match_id
    private String lolMatchId;         // lolesports match ID
    private LocalDate lolScheduledDate; // LOL 경기 날짜 (정산 시 API 재조회용)
    private String sportCode;          // baseball / soccer / lol
    private String predictedResult;    // HOME_WIN / DRAW / AWAY_WIN
}

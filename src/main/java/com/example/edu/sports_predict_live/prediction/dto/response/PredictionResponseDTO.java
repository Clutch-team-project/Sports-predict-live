package com.example.edu.sports_predict_live.prediction.dto.response;

import com.example.edu.sports_predict_live.prediction.entity.Prediction;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class PredictionResponseDTO {

    private final Long    predictionId;
    private final String  sportCode;
    private final String  predictedResult;
    private final String  actualResult;
    private final Boolean isCorrect;
    private final int     pointsEarned;

    // 경기 정보
    private final String homeTeamName;
    private final String awayTeamName;
    private final String matchStatus;   // scheduled / in_progress / finished
    private final String scheduledAt;   // "yyyy-MM-dd HH:mm"
    private final String createdAt;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PredictionResponseDTO(Prediction p) {
        this.predictionId    = p.getPredictionId();
        this.sportCode       = p.getSportCode();
        this.predictedResult = p.getPredictedResult();
        this.actualResult    = p.getActualResult();
        this.isCorrect       = p.getIsCorrect();
        this.pointsEarned    = p.getPointsEarned();
        this.createdAt       = p.getCreatedAt().format(DT_FMT);

        if (p.getMatch() != null) {
            this.homeTeamName = p.getMatch().getHomeTeam().getName();
            this.awayTeamName = p.getMatch().getAwayTeam().getName();
            this.matchStatus  = p.getMatch().getStatus();
            this.scheduledAt  = p.getMatch().getScheduledAt().format(DT_FMT);
        } else {
            // LOL — 팀 정보 없이 반환 (프론트에서 matchId로 조회)
            this.homeTeamName = null;
            this.awayTeamName = null;
            this.matchStatus  = null;
            this.scheduledAt  = p.getLolScheduledDate() != null
                    ? p.getLolScheduledDate().toString()
                    : null;
        }
    }
}

package com.example.edu.sports_predict_live.prediction.dto.response;

public record PredictionSummaryDTO(
        Long matchId,
        String homeTeamName,
        String awayTeamName,
        long homeVoteCount,
        long awayVoteCount,
        long drawVoteCount,
        long totalVoteCount,
        int homePercent,
        int awayPercent,
        int drawPercent,
        boolean closed
) {
}

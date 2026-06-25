package com.example.edu.sports_predict_live.match.dto;

import com.example.edu.sports_predict_live.match.entity.Match;

import java.time.LocalDateTime;

public record MatchListDTO(
        Long gameId,
        Long sportId,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        LocalDateTime scheduledAt,
        String status,
        Integer homeScore,
        Integer awayScore
) {
    public static MatchListDTO from(Match match) {
        return new MatchListDTO(
                match.getMatchId(),
                match.getSport().getSportId(),
                match.getHomeTeam().getTeamId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getTeamId(),
                match.getAwayTeam().getName(),
                match.getScheduledAt(),
                match.getStatus(),
                match.getHomeScore(),
                match.getAwayScore()
        );
    }
}

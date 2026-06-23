package com.example.edu.sports_predict_live.match.dto;

import com.example.edu.sports_predict_live.match.entity.Match;

import java.time.LocalDateTime;

public record MatchDTO(
        Long gameId,
        Long sportId,
        String sportCode,
        Long homeTeamId,
        String homeTeamName,
        String homeTeamEmblem,
        Long awayTeamId,
        String awayTeamName,
        String awayTeamEmblem,
        LocalDateTime scheduledAt,
        String status,
        Integer homeScore,
        Integer awayScore,
        String season,
        String venue
) {
    public static MatchDTO from(Match match) {
        return new MatchDTO(
                match.getMatchId(),
                match.getSport().getSportId(),
                match.getSport().getCode(),
                match.getHomeTeam().getTeamId(),
                match.getHomeTeam().getName(),
                match.getHomeTeam().getEmblemUrl(),
                match.getAwayTeam().getTeamId(),
                match.getAwayTeam().getName(),
                match.getAwayTeam().getEmblemUrl(),
                match.getScheduledAt(),
                match.getStatus(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getSeason(),
                match.getVenue()
        );
    }
}

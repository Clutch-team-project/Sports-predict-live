package com.example.edu.sports_predict_live.match.dto.response;

import com.example.edu.sports_predict_live.match.entity.Match;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
public class MatchResponseDTO {

    private final Long   matchId;
    private final Long   homeTeamId;
    private final Long   awayTeamId;
    private final String homeTeamName;
    private final String awayTeamName;
    private final String homeTeamEmblem;
    private final String awayTeamEmblem;
    private final int    homeScore;
    private final int    awayScore;
    private final String status;      // scheduled / in_progress / finished / cancelled
    private final String scheduledAt; // "HH:mm"
    private final String venue;
    private final String date;        // "YYYY-MM-DD"

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public MatchResponseDTO(Match match) {
        this.matchId         = match.getMatchId();
        this.homeTeamId      = match.getHomeTeam().getTeamId();
        this.awayTeamId      = match.getAwayTeam().getTeamId();
        this.homeTeamName    = match.getHomeTeam().getName();
        this.awayTeamName    = match.getAwayTeam().getName();
        this.homeTeamEmblem  = match.getHomeTeam().getEmblemUrl();
        this.awayTeamEmblem  = match.getAwayTeam().getEmblemUrl();
        this.homeScore       = match.getHomeScore();
        this.awayScore       = match.getAwayScore();
        this.status          = match.getStatus();
        this.scheduledAt     = match.getScheduledAt().format(TIME_FMT);
        this.venue           = match.getVenue();
        this.date            = match.getScheduledAt().format(DATE_FMT);
    }
}

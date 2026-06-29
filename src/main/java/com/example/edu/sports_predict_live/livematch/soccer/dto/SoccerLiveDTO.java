package com.example.edu.sports_predict_live.livematch.soccer.dto;

import com.example.edu.sports_predict_live.prediction.dto.response.PredictionSummaryDTO;
import com.example.edu.sports_predict_live.team.dto.response.StandingsResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public record SoccerLiveDTO(
        Long matchId,
        String sport,
        String status,
        LocalDateTime scheduledAt,
        Scoreboard scoreboard,
        Timeline timeline,
        Records records,
        Lineup lineup,
        StandingsResponseDTO homeSeasonStat,
        StandingsResponseDTO awaySeasonStat,
        List<SameDateGame> sameDateGames,
        List<GoalScorer> goalScorers,
        PredictionSummaryDTO prediction,
        List<Object> comments
) {
    public record Scoreboard(
            Long homeTeamId,
            Long awayTeamId,
            String homeTeamName,
            String awayTeamName,
            String homeTeamEmblem,
            String awayTeamEmblem,
            int homeScore,
            int awayScore,
            String stadium,
            String matchDate,
            String matchTime,
            String currentMinute,
            String phase,
            String homeFormation,
            String awayFormation
    ) {
    }

    public record Timeline(List<TimelineSection> sections) {
    }

    public record SameDateGame(
            Long matchId,
            String status,
            String matchTime,
            String homeTeamName,
            String awayTeamName,
            String homeTeamEmblem,
            String awayTeamEmblem,
            int homeScore,
            int awayScore
    ) {
    }

    public record GoalScorer(
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String minute,
            boolean ownGoal
    ) {
    }

    public record TimelineSection(
            String key,
            String title,
            String scoreText,
            boolean open,
            List<TimelineEvent> events
    ) {
    }

    public record TimelineEvent(
            Long eventId,
            String eventType,
            String label,
            String icon,
            String minute,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String description,
            int homeScore,
            int awayScore
    ) {
    }

    public record Records(TeamRecords team, PlayerRecords players) {
    }

    public record TeamRecords(TeamStat home, TeamStat away) {
    }

    public record TeamStat(
            Long teamId,
            String teamName,
            int goals,
            int passes,
            int shots,
            int shotsOnTarget,
            int cornerKicks,
            int fouls,
            int offsides,
            int yellowCards,
            int redCards,
            int saves
    ) {
    }

    public record PlayerRecords(List<PlayerStat> home, List<PlayerStat> away) {
    }

    public record PlayerStat(
            Long playerId,
            String playerName,
            Long teamId,
            String teamName,
            Integer orderNum,
            String position,
            boolean starter,
            int minutes,
            int goals,
            int assists,
            int passes,
            int shots,
            int shotsOnTarget,
            int fouls,
            int offsides,
            int yellowCards,
            int redCards,
            int saves
    ) {
    }

    public record Lineup(LineupTeam home, LineupTeam away) {
    }

    public record LineupTeam(
            Long teamId,
            String teamName,
            String emblemUrl,
            String formation,
            List<LineupPlayer> starters,
            List<LineupPlayer> bench
    ) {
    }

    public record LineupPlayer(
            Long playerId,
            String playerName,
            Integer orderNum,
            String position,
            boolean starter,
            Integer jerseyNumber
    ) {
    }
}

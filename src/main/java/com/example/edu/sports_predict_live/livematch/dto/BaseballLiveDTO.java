package com.example.edu.sports_predict_live.livematch.dto;

import java.util.List;

public record BaseballLiveDTO(
        Long matchId,
        String status,
        String currentPeriod,
        TeamInfo homeTeam,
        TeamInfo awayTeam,
        Scoreboard scoreboard,
        Count count,
        BaseState baseState,
        PitcherGameStat currentPitcher,
        List<Fielder> fielders,
        List<LineupPlayer> onDeck,
        List<AtBatCard> timeline,
        List<PlayerGameStat> playerGameStats,
        List<PitcherGameStat> pitcherGameStats,
        List<MatchLineupDTO> lineup
) {

    public record TeamInfo(Long teamId, String name, String logoText) {
    }

    public record Scoreboard(
            int homeScore,
            int awayScore,
            List<InningScore> inningScores,
            int homeHits,
            int awayHits,
            int homeErrors,
            int awayErrors,
            int homeWalks,
            int awayWalks
    ) {
    }

    public record InningScore(int inning, int home, int away) {
    }

    public record Count(int balls, int strikes, int outs) {
    }

    public record BaseState(BaseRunner first, BaseRunner second, BaseRunner third) {
    }

    public record BaseRunner(Long playerId, String name) {
    }

    public record PitcherGameStat(
            Long playerId,
            String name,
            Long teamId,
            String teamName,
            String position,
            String inningsPitched,
            int pitchCount,
            int hitsAllowed,
            int strikeouts,
            int walksAllowed,
            int outsPitched
    ) {
    }

    public record Fielder(Long playerId, String name, String playerName, Long teamId, String position) {
    }

    public record LineupPlayer(
            Long playerId,
            String name,
            String playerName,
            Long teamId,
            Integer orderNum,
            String position,
            boolean starter
    ) {
    }

    public record AtBatCard(
            String period,
            Long batterId,
            String batterName,
            Integer orderNum,
            String position,
            boolean current,
            String resultLabel,
            String summary,
            PlayerGameStat stat,
            List<PitchRow> pitches
    ) {
    }

    public record PitchRow(
            Integer pitchNo,
            String eventType,
            String label,
            String description,
            String countText,
            boolean actualPitch,
            boolean terminal
    ) {
    }

    public record PlayerGameStat(
            Long playerId,
            String name,
            Long teamId,
            Integer orderNum,
            String position,
            int plateAppearances,
            int atBats,
            int hits,
            int runs,
            int rbi,
            int walks,
            int strikeouts,
            int steals
    ) {
    }
}

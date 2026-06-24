package com.example.edu.sports_predict_live.livematch.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class BaseballAdminLineupDTO {

    private BaseballAdminLineupDTO() {
    }

    public record TeamInfo(
            Long teamId,
            String name,
            String emblemUrl
    ) {
    }

    public record PlayerOption(
            Long playerId,
            Long teamId,
            String teamName,
            String name,
            String position,
            Integer jerseyNumber,
            boolean pitcher
    ) {
    }

    public record LineupEntry(
            Long matchLineupId,
            Long matchId,
            Long teamId,
            String teamName,
            Long playerId,
            String playerName,
            String playerPosition,
            Integer jerseyNumber,
            boolean starter,
            Integer orderNum,
            String position,
            boolean included,
            boolean pitcher
    ) {
    }

    public record LineupStatus(
            Long matchId,
            boolean ready,
            boolean homeReady,
            boolean awayReady,
            int homeStarterCount,
            int awayStarterCount,
            int homePitcherCount,
            int awayPitcherCount,
            String message
    ) {
    }

    public record Editor(
            Long matchId,
            String status,
            LocalDateTime scheduledAt,
            String venue,
            TeamInfo homeTeam,
            TeamInfo awayTeam,
            LineupStatus lineupStatus,
            List<PlayerOption> homePlayers,
            List<PlayerOption> awayPlayers,
            List<LineupEntry> homeLineup,
            List<LineupEntry> awayLineup
    ) {
    }

    public record SaveRequest(
            List<SaveEntry> entries
    ) {
    }

    public record SaveEntry(
            Long teamId,
            Long playerId,
            Boolean included,
            Boolean starter,
            Integer orderNum,
            String position
    ) {
    }
}

package com.example.edu.sports_predict_live.livematch.dto;

import com.example.edu.sports_predict_live.livematch.entity.MatchPlayerStat;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.team.entity.Team;

import java.math.BigDecimal;
import java.util.List;

public record MatchPlayerStatDTO(
        Long playerId,
        String playerName,
        Long teamId,
        String teamName,
        String position,
        List<StatDTO> stats
) {

    public record StatDTO(
            Long matchPlayerStatId,
            Long matchId,
            String statKey,
            BigDecimal statValue
    ) {
        public static StatDTO from(MatchPlayerStat stat) {
            return new StatDTO(
                    stat.getMatchPlayerStatId(),
                    stat.getMatchId(),
                    stat.getStatKey(),
                    stat.getStatValue()
            );
        }
    }

    public static MatchPlayerStatDTO from(Player player, Team team, List<MatchPlayerStat> stats) {
        MatchPlayerStat first = stats.get(0);

        return new MatchPlayerStatDTO(
                first.getPlayerId(),
                player != null ? player.getName() : null,
                first.getTeamId(),
                team != null ? team.getName() : null,
                player != null ? player.getPosition() : null,
                stats.stream().map(StatDTO::from).toList()
        );
    }
}

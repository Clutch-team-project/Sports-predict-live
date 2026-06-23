package com.example.edu.sports_predict_live.livematch.dto;

import com.example.edu.sports_predict_live.livematch.entity.MatchLineup;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.team.entity.Team;

public record MatchLineupDTO(
        Long matchLineupId,
        Long matchId,
        Long teamId,
        String teamName,
        Long playerId,
        String playerName,
        boolean starter,
        Integer orderNum,
        String position
) {

    public static MatchLineupDTO from(MatchLineup lineup, Team team, Player player) {
        return new MatchLineupDTO(
                lineup.getMatchLineupId(),
                lineup.getMatchId(),
                lineup.getTeamId(),
                team != null ? team.getName() : null,
                lineup.getPlayerId(),
                player != null ? player.getName() : null,
                lineup.isStarter(),
                lineup.getOrderNum(),
                lineup.getPosition()
        );
    }
}
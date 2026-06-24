package com.example.edu.sports_predict_live.livematch.stat.dto;

import com.example.edu.sports_predict_live.livematch.stat.entity.MatchTeamStat;
import com.example.edu.sports_predict_live.team.entity.Team;

import java.math.BigDecimal;

public record MatchTeamStatDTO(
        Long matchTeamStatId,
        Long matchId,
        Long teamId,
        String teamName,
        String statKey,
        BigDecimal statValue
) {

    public static MatchTeamStatDTO from(MatchTeamStat stat, Team team) {
        return new MatchTeamStatDTO(
                stat.getMatchTeamStatId(),
                stat.getMatchId(),
                stat.getTeamId(),
                team != null ? team.getName() : null,
                stat.getStatKey(),
                stat.getStatValue()
        );
    }
}

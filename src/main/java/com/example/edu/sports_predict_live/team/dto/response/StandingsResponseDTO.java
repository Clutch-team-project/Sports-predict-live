package com.example.edu.sports_predict_live.team.dto.response;

import com.example.edu.sports_predict_live.team.entity.TeamSeasonStat;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StandingsResponseDTO {

    private final Long teamId;
    private final String teamName;
    private final String emblemUrl;
    private final Integer rank;
    private final int wins;
    private final int draws;
    private final int losses;
    private final BigDecimal winRate;
    private final int pointsFor;
    private final int pointsAgainst;
    private final String recentForm;  // 예: "WWDLW"

    public StandingsResponseDTO(TeamSeasonStat stat) {
        this.teamId       = stat.getTeam().getTeamId();
        this.teamName     = stat.getTeam().getName();
        this.emblemUrl    = stat.getTeam().getEmblemUrl();
        this.rank         = stat.getRank();
        this.wins         = stat.getWins();
        this.draws        = stat.getDraws();
        this.losses       = stat.getLosses();
        this.winRate      = stat.getWinRate();
        this.pointsFor    = stat.getPointsFor();
        this.pointsAgainst = stat.getPointsAgainst();
        this.recentForm   = stat.getRecentForm();
    }
}

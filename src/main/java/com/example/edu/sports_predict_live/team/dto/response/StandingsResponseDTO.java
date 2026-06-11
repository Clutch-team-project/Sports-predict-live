package com.example.edu.sports_predict_live.team.dto.response;

import com.example.edu.sports_predict_live.team.entity.TeamSeasonStat;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private final String recentForm;

    public StandingsResponseDTO(TeamSeasonStat stat) {
        this.teamId        = stat.getTeam().getTeamId();
        this.teamName      = stat.getTeam().getName();
        this.emblemUrl     = stat.getTeam().getEmblemUrl();
        this.rank          = stat.getRank();
        this.wins          = stat.getWins();
        this.draws         = stat.getDraws();
        this.losses        = stat.getLosses();
        this.winRate       = stat.getWinRate();
        this.pointsFor     = stat.getPointsFor();
        this.pointsAgainst = stat.getPointsAgainst();
        this.recentForm    = stat.getRecentForm();
    }

    // lolesports API 응답으로 생성 (LoL 전용)
    public StandingsResponseDTO(int rank, String teamName, String emblemUrl, int wins, int losses) {
        this.teamId        = null;
        this.teamName      = teamName;
        this.emblemUrl     = emblemUrl;
        this.rank          = rank;
        this.wins          = wins;
        this.draws         = 0;
        this.losses        = losses;
        int total          = wins + losses;
        this.winRate       = total > 0
                ? BigDecimal.valueOf(wins).divide(BigDecimal.valueOf(total), 3, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.pointsFor     = 0;
        this.pointsAgainst = 0;
        this.recentForm    = null;
    }
}

package com.example.edu.sports_predict_live.player.dto.response;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class HitterRecordResponseDTO {

    private final Long playerId;
    private final String playerName;
    private final String teamName;
    private final String profileImage;
    private final int gamesPlayed;
    private final BigDecimal battingAvg;
    private final Integer hits;
    private final Integer homeRuns;
    private final Integer rbi;
    private final boolean qualified;

    public HitterRecordResponseDTO(PlayerSeasonStatBaseball stat) {
        var pss    = stat.getPlayerSeasonStat();
        var player = pss.getPlayer();
        this.playerId     = player.getPlayerId();
        this.playerName   = player.getName();
        this.teamName     = player.getTeam().getName();
        this.profileImage = player.getProfileImage();
        this.gamesPlayed  = pss.getGamesPlayed();
        this.battingAvg   = stat.getBattingAvg();
        this.hits         = stat.getHits();
        this.homeRuns     = stat.getHomeRuns();
        this.rbi          = stat.getRbi();
        this.qualified    = Boolean.TRUE.equals(stat.getQualified());
    }
}

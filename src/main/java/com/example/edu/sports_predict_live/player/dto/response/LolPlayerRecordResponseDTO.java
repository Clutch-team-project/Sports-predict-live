package com.example.edu.sports_predict_live.player.dto.response;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatLol;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class LolPlayerRecordResponseDTO {

    private final Long       playerId;
    private final String     playerName;
    private final String     teamName;
    private final String     position;
    private final String     profileImage;
    private final int        gamesPlayed;
    private final BigDecimal kda;
    private final BigDecimal avgKills;
    private final BigDecimal avgDeaths;
    private final BigDecimal avgAssists;
    private final BigDecimal winRate;

    public LolPlayerRecordResponseDTO(PlayerSeasonStatLol stat) {
        var pss    = stat.getPlayerSeasonStat();
        var player = pss.getPlayer();
        this.playerId     = player.getPlayerId();
        this.playerName   = player.getName();
        this.teamName     = player.getTeam().getName();
        this.position     = player.getPosition();
        this.profileImage = player.getProfileImage();
        this.gamesPlayed  = pss.getGamesPlayed();
        this.kda          = stat.getKda();
        this.avgKills     = stat.getAvgKills();
        this.avgDeaths    = stat.getAvgDeaths();
        this.avgAssists   = stat.getAvgAssists();
        this.winRate      = stat.getWinRate();
    }
}

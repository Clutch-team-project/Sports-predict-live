package com.example.edu.sports_predict_live.player.dto.response;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PitcherRecordResponseDTO {

    private final Long playerId;
    private final String playerName;
    private final String teamName;
    private final String profileImage;
    private final int gamesPlayed;
    private final BigDecimal era;
    private final Integer wins;
    private final Integer losses;
    private final Integer strikeouts;
    private final Integer saves;
    private final Integer holds;

    public PitcherRecordResponseDTO(PlayerSeasonStatBaseball stat) {
        var pss    = stat.getPlayerSeasonStat();
        var player = pss.getPlayer();
        this.playerId     = player.getPlayerId();
        this.playerName   = player.getName();
        this.teamName     = player.getTeam().getName();
        this.profileImage = player.getProfileImage();
        this.gamesPlayed  = pss.getGamesPlayed();
        this.era          = stat.getEra();
        this.wins         = stat.getWins();
        this.losses       = stat.getLosses();
        this.strikeouts   = stat.getStrikeouts();
        this.saves        = stat.getSaves();
        this.holds        = stat.getHolds();
    }
}

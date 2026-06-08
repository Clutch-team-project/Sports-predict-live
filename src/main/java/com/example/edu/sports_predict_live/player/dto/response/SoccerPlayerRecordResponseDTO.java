package com.example.edu.sports_predict_live.player.dto.response;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatSoccer;
import lombok.Getter;

@Getter
public class SoccerPlayerRecordResponseDTO {

    private final Long   playerId;
    private final String playerName;
    private final String teamName;
    private final String profileImage;
    private final int    gamesPlayed;
    private final int    goals;
    private final int    assists;
    private final int    yellowCards;
    private final int    redCards;
    private final int    cleanSheets;

    public SoccerPlayerRecordResponseDTO(PlayerSeasonStatSoccer stat) {
        var pss    = stat.getPlayerSeasonStat();
        var player = pss.getPlayer();
        this.playerId     = player.getPlayerId();
        this.playerName   = player.getName();
        this.teamName     = player.getTeam().getName();
        this.profileImage = player.getProfileImage();
        this.gamesPlayed  = pss.getGamesPlayed();
        this.goals        = stat.getGoals();
        this.assists      = stat.getAssists();
        this.yellowCards  = stat.getYellowCards();
        this.redCards     = stat.getRedCards();
        this.cleanSheets  = stat.getCleanSheets();
    }
}
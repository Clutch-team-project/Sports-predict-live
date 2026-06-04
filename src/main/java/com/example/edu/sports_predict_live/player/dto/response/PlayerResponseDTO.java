package com.example.edu.sports_predict_live.player.dto.response;

import com.example.edu.sports_predict_live.player.entity.Player;
import lombok.Getter;

@Getter
public class PlayerResponseDTO {

    private final Long   playerId;
    private final String playerName;
    private final String teamName;
    private final String position;
    private final String profileImage;

    public PlayerResponseDTO(Player player) {
        this.playerId     = player.getPlayerId();
        this.playerName   = player.getName();
        this.teamName     = player.getTeam().getName();
        this.position     = player.getPosition();
        this.profileImage = player.getProfileImage();
    }
}

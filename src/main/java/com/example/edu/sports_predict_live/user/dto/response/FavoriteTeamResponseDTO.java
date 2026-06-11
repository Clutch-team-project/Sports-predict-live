package com.example.edu.sports_predict_live.user.dto.response;

import com.example.edu.sports_predict_live.user.entity.UserFavoriteTeam;
import lombok.Getter;

@Getter
public class FavoriteTeamResponseDTO {

    private final Long teamId;
    private final String teamName;
    private final String sport;
    private final String emblemUrl;

    public FavoriteTeamResponseDTO(UserFavoriteTeam uft) {
        this.teamId   = uft.getTeam().getTeamId();
        this.teamName = uft.getTeam().getName();
        this.sport    = uft.getTeam().getSport().getName();
        this.emblemUrl = uft.getTeam().getEmblemUrl();
    }
}

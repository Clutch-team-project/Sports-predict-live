package com.example.edu.sports_predict_live.team.dto.response;

import com.example.edu.sports_predict_live.team.entity.Team;
import lombok.Getter;

@Getter
public class TeamSimpleResponseDTO {

    private final Long   teamId;
    private final String name;
    private final String emblemUrl;

    public TeamSimpleResponseDTO(Team team) {
        this.teamId    = team.getTeamId();
        this.name      = team.getName();
        this.emblemUrl = team.getEmblemUrl();
    }
}

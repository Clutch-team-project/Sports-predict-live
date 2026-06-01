package com.example.edu.sports_predict_live.aiprediction.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiPredBasisDto {
    private Double home_season;
    private Double away_season;
    private Double head_to_head;
    private Double home_advantage;
}

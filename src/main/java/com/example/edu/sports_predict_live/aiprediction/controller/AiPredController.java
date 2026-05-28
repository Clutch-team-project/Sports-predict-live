package com.example.edu.sports_predict_live.controller;

import com.example.edu.sports_predict_live.entity.AiPredEntity;
import com.example.edu.sports_predict_live.service.AiPredService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-pred")
@RequiredArgsConstructor
public class AiPredController {

    private final AiPredService aiPredService;

    @GetMapping("/save")
    public AiPredEntity saveAiPrediction(

            @RequestParam Long matchId,
            @RequestParam Double homeWinProb,
            @RequestParam Double drawProb,
            @RequestParam Double awayWinProb
    ) {

        return aiPredService.saveAiPrediction(
                matchId,
                homeWinProb,
                drawProb,
                awayWinProb
        );
    }

    @GetMapping("/match")
    public AiPredEntity getAiPrediction(
            @RequestParam Long matchId
    ) {

        return aiPredService.getAiPrediction(matchId);
    }
}
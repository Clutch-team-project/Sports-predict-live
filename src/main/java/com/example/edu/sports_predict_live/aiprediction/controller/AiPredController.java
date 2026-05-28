package com.example.edu.sports_predict_live.aiprediction.controller;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.service.AiPredService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-prediction")
@RequiredArgsConstructor
public class AiPredController {

    private final AiPredService aiPredService;

    @PostMapping("/save")
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

    @GetMapping("/{matchId}")
    public AiPredEntity getAiPrediction(
            @PathVariable Long matchId
    ) {

        return aiPredService.getAiPrediction(matchId);
    }
}
package com.example.edu.sports_predict_live.aiprediction.controller;

import com.example.edu.sports_predict_live.aiprediction.entity.PredictionEntity;
import com.example.edu.sports_predict_live.aiprediction.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.edu.sports_predict_live.aiprediction.dto.PredictionRankingDto;


import java.util.List;

@RestController
@RequestMapping("/prediction")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/save") //실제 db연결 시 포스트 변경
    public PredictionEntity savePrediction(

            @RequestParam Long userId,
            @RequestParam Long matchId,
            @RequestParam String predictedResult
    ) {

        return predictionService.savePrediction(
                userId,
                matchId,
                predictedResult
        );
    }

    @GetMapping("/user")
    public List<PredictionEntity> getUserPredictions(
            @RequestParam Long userId
    ) {

        return predictionService.getUserPredictions(userId);
    }

    @PutMapping("/check")
    public PredictionEntity updatePredictionResult(

            @RequestParam Long predictionId,
            @RequestParam boolean isCorrect
    ) {

        return predictionService.updatePredictionResult(
                predictionId,
                isCorrect
        );
    }

    @GetMapping("/all")
    public List<PredictionEntity> getAllPredictions() {

        return predictionService.getAllPredictions();


    }

    @GetMapping("/{predictionId}")
    public PredictionEntity getPrediction(
            @PathVariable Long predictionId
    ) {

        return predictionService.getPrediction(predictionId);

    }

    @GetMapping("/ranking")
    public List<PredictionRankingDto> getPredictionRanking() {

        return predictionService.getPredictionRanking();
    }

}
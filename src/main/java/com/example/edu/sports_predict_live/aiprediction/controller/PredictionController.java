package com.example.edu.sports_predict_live.aiprediction.controller;

import com.example.edu.sports_predict_live.aiprediction.entity.PredictionEntity;
import com.example.edu.sports_predict_live.aiprediction.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prediction")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/save")
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
}
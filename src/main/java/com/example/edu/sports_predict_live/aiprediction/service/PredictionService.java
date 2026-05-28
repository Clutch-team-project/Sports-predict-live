package com.example.edu.sports_predict_live.service;

import com.example.edu.sports_predict_live.entity.PredictionEntity;
import com.example.edu.sports_predict_live.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;

    public PredictionEntity savePrediction(
            Long userId,
            Long matchId,
            String predictedResult
    ) {

        // 이미 예측한 경기인지 확인
        boolean exists =
                predictionRepository.existsByUserIdAndMatchId(
                        userId,
                        matchId
                );

        if (exists) {
            throw new RuntimeException("이미 예측한 경기입니다.");
        }

        PredictionEntity prediction = new PredictionEntity();

        prediction.setUserId(userId);
        prediction.setMatchId(matchId);
        prediction.setPredictedResult(predictedResult);

        // 경기 끝나기 전이라 기본 false
        prediction.setIsCorrect(false);

        prediction.setCreatedAt(LocalDateTime.now());

        return predictionRepository.save(prediction);
    }

    public List<PredictionEntity> getUserPredictions(
            Long userId
    ) {

        return predictionRepository.findByUserId(userId);
    }

    public PredictionEntity updatePredictionResult(
            Long predictionId,
            boolean isCorrect
    ) {

        PredictionEntity prediction =
                predictionRepository.findByPredictionId(predictionId);

        prediction.setIsCorrect(isCorrect);

        return predictionRepository.save(prediction);
    }
}
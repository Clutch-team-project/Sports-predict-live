package com.example.edu.sports_predict_live.aiprediction.repository;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.entity.PredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PredictionRepository

        extends JpaRepository<PredictionEntity, Long> {
    boolean existsByUserIdAndMatchId(
            Long userId,
            Long matchId
    );
    List<PredictionEntity> findByUserId(Long userId);

    PredictionEntity findByPredictionId(Long predictionId);

    List<PredictionEntity> findByMatchId(Long matchId);
}
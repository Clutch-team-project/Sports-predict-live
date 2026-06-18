package com.example.edu.sports_predict_live.aiprediction.repository;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {

    Optional<AiPrediction> findByMatch_MatchId(Long matchId);

    Optional<AiPrediction> findByLolMatchId(String lolMatchId);
}

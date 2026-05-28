package com.example.edu.sports_predict_live.aiprediction.repository;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository
        extends JpaRepository<MatchEntity, Long> {
}
package com.example.edu.sports_predict_live.aiprediction.repository;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPredRepository
        extends JpaRepository<AiPredEntity, Long> {

    boolean existsByMatchId(Long matchId); //ai예측 같은경기 중복예측 방지
    AiPredEntity findByMatchId(Long matchId);
    AiPredEntity findTopByOrderByCreatedAtDesc(); //가장 최근 ai 예측 1개 조회

}
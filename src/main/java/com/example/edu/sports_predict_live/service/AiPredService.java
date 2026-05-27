package com.example.edu.sports_predict_live.service;

import com.example.edu.sports_predict_live.entity.AiPredEntity;
import com.example.edu.sports_predict_live.repository.AiPredRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiPredService {

    private final AiPredRepository aiPredRepository;

    public AiPredEntity saveAiPrediction(
            Long matchId,
            Double homeWinProb,
            Double drawProb,
            Double awayWinProb
    ) {

        // 경기당 AI 예측 1개만 허용
        boolean exists =
                aiPredRepository.existsByMatchId(matchId);

        if (exists) {
            throw new RuntimeException("이미 AI 예측이 존재합니다.");
        }

        AiPredEntity aiPred = new AiPredEntity();

        aiPred.setMatchId(matchId);

        aiPred.setHomeWinProb(homeWinProb);
        aiPred.setDrawProb(drawProb);
        aiPred.setAwayWinProb(awayWinProb);

        aiPred.setCreatedAt(LocalDateTime.now());

        return aiPredRepository.save(aiPred);
    }
    public AiPredEntity getAiPrediction(Long matchId) {

        return aiPredRepository.findByMatchId(matchId);
    }
}
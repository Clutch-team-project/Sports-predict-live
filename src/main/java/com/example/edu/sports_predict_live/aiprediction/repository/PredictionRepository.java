package com.example.edu.sports_predict_live.aiprediction.repository;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.entity.PredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.edu.sports_predict_live.aiprediction.dto.PredictionRankingDto;
import org.springframework.data.jpa.repository.Query;


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

    @Query(value = """
SELECT
user_id AS userId,
COUNT(*) AS totalPredictions,
SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) AS correctPredictions,
ROUND(
(SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) * 100.0) / COUNT(*),
2
) AS accuracy
FROM prediction
WHERE is_correct IS NOT NULL
GROUP BY user_id
ORDER BY accuracy DESC
""", nativeQuery = true)
    List<Object[]> getPredictionRanking();

}
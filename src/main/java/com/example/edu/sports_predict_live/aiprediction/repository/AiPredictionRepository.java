package com.example.edu.sports_predict_live.aiprediction.repository;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AiPredictionRepository extends JpaRepository<AiPrediction, Long> {

    Optional<AiPrediction> findByMatch_MatchId(Long matchId);

    Optional<AiPrediction> findByLolMatchId(String lolMatchId);

    // 홈 통계 카드용: 종료된 DB 경기(야구·축구) 기준 AI 예측 건수 + 적중 수
    @Query("SELECT COUNT(a), " +
           "SUM(CASE WHEN " +
           "  (a.homeWinProb >= a.awayWinProb AND a.homeWinProb >= a.drawProb AND a.match.homeScore > a.match.awayScore) OR " +
           "  (a.awayWinProb > a.homeWinProb AND a.awayWinProb > a.drawProb AND a.match.awayScore > a.match.homeScore) OR " +
           "  (a.drawProb > a.homeWinProb AND a.drawProb > a.awayWinProb AND a.match.homeScore = a.match.awayScore) " +
           "THEN 1L ELSE 0L END) " +
           "FROM AiPrediction a WHERE a.match IS NOT NULL AND a.match.status = 'finished'")
    List<Object[]> findAiAccuracyStats();
}

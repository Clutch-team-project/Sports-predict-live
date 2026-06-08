package com.example.edu.sports_predict_live.aiprediction.service;

import com.example.edu.sports_predict_live.aiprediction.dto.AiPredBasisDto;
import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.entity.MatchEntity;
import com.example.edu.sports_predict_live.aiprediction.repository.AiPredRepository;
import com.example.edu.sports_predict_live.aiprediction.repository.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiPredService {

    // 종목 ID 기준
    private static final Long SPORT_SOCCER = 1L;
    private static final Long SPORT_BASEBALL = 2L;
    private static final Long SPORT_LOL = 3L;

    // 통계 DB 연동 전 임시 기본값
    private static final double DEFAULT_HOME_SEASON_SCORE = 0.55;
    private static final double DEFAULT_AWAY_SEASON_SCORE = 0.55;
    private static final double DEFAULT_HEAD_TO_HEAD_SCORE = 0.15;
    private static final double DEFAULT_HOME_ADVANTAGE = 0.10;

    // 무승부 기본 가중치
    private static final double DEFAULT_DRAW_WEIGHT = 0.7;

    private final AiPredRepository aiPredRepository;
    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;

    @Transactional
    public AiPredEntity calculateAndSaveAiPrediction(Long matchId) {

        // 1. 경기당 AI 예측 1개만 허용
        AiPredEntity existingPrediction =
                aiPredRepository.findByMatchId(matchId);

        if (existingPrediction != null) {
            return existingPrediction;
        }

        // 2. 경기 정보 조회
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 경기입니다."));

        // 3. 임시 더미 통계값
        Double homeSeasonScore = DEFAULT_HOME_SEASON_SCORE;
        Double awaySeasonScore = DEFAULT_AWAY_SEASON_SCORE;

        Double headToHeadScore = DEFAULT_HEAD_TO_HEAD_SCORE;
        Double homeAdvantage = DEFAULT_HOME_ADVANTAGE;
        // 가중치 계산
        double homeWeight = 1.0 + homeSeasonScore + headToHeadScore + homeAdvantage;
        double awayWeight = 1.0 + awaySeasonScore;

        // 무승부 가중치
        double drawWeight = DEFAULT_DRAW_WEIGHT;

        // LoL 같은 무승부 없는 종목 처리
        if (SPORT_LOL.equals(match.getSportId())) {
            drawWeight = 0.0;
        }

        double totalWeight = homeWeight + drawWeight + awayWeight;

        // 확률 계산
        BigDecimal homeProb = BigDecimal.valueOf(homeWeight / totalWeight)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal drawProb = BigDecimal.valueOf(drawWeight / totalWeight)
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal awayProb = BigDecimal.ONE
                .subtract(homeProb)
                .subtract(drawProb);

        // basis JSON 생성
        String basisJson;

        try {
            AiPredBasisDto basisDto = new AiPredBasisDto();

            basisDto.setHome_season(homeSeasonScore);
            basisDto.setAway_season(awaySeasonScore);
            basisDto.setHead_to_head(headToHeadScore);
            basisDto.setHome_advantage(homeAdvantage);
            basisDto.setSport_id(match.getSportId());
            basisDto.setDraw_weight(drawWeight);

            basisJson = objectMapper.writeValueAsString(basisDto);

        } catch (Exception e) {

            basisJson = "{}";
        }

        // 저장
        AiPredEntity aiPred = new AiPredEntity();

        aiPred.setMatchId(matchId);
        aiPred.setHomeWinProb(homeProb.doubleValue());
        aiPred.setDrawProb(drawProb.doubleValue());
        aiPred.setAwayWinProb(awayProb.doubleValue());

        aiPred.setBasis(basisJson);
        aiPred.setCreatedAt(LocalDateTime.now());

        return aiPredRepository.save(aiPred);
    }

    /**
     * 경기별 AI 예측 조회
     */
    public AiPredEntity getAiPrediction(Long matchId) {

        AiPredEntity prediction =
                aiPredRepository.findByMatchId(matchId);

        if (prediction == null) {
            throw new RuntimeException("AI 예측 데이터가 존재하지 않습니다.");
        }

        return prediction;
    }

    /**
     * 최신 AI 예측 조회
     */
    public AiPredEntity getLatestPrediction() {

        AiPredEntity prediction =
                aiPredRepository.findTopByOrderByCreatedAtDesc();

        if (prediction == null) {
            throw new RuntimeException("AI 예측 데이터가 존재하지 않습니다.");
        }

        return prediction;
    }
}

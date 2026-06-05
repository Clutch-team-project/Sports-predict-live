package com.example.edu.sports_predict_live.aiprediction.service;

import com.example.edu.sports_predict_live.aiprediction.entity.MatchEntity;
import com.example.edu.sports_predict_live.aiprediction.entity.PredictionEntity;
import com.example.edu.sports_predict_live.aiprediction.repository.MatchRepository;
import com.example.edu.sports_predict_live.aiprediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.edu.sports_predict_live.aiprediction.dto.PredictionRankingDto;
import java.util.ArrayList;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;

    /**
     * 사용자 승부 예측 등록
     */
    public PredictionEntity savePrediction(
            Long userId,
            Long matchId,
            String predictedResult
    ) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("경기 없음"));

        if (predictedResult == null
                || (!predictedResult.equals("home")
                && !predictedResult.equals("draw")
                && !predictedResult.equals("away"))) {

            throw new RuntimeException("예측값은 home, draw, away 중 하나여야 합니다.");
        }

        // LOL은 무승부 예측 불가
        if (Long.valueOf(3L).equals(match.getSportId())
                && predictedResult.equals("draw")) {
            throw new RuntimeException("LOL 경기는 무승부 예측이 불가능합니다.");
        }
        // 이미 예측한 경기인지 확인
        boolean exists = predictionRepository.existsByUserIdAndMatchId(userId, matchId);

        if (exists) {
            throw new RuntimeException("이미 예측한 경기입니다.");
        }

        PredictionEntity prediction = new PredictionEntity();
        prediction.setUserId(userId);
        prediction.setMatchId(matchId);
        prediction.setPredictedResult(predictedResult);

        // 🛠️ [수정] 기본값을 false가 아닌 null로 처리해야 명세서의 'NULL: 미정' 규칙에 맞습니다.
        prediction.setIsCorrect(null);

        prediction.setCreatedAt(LocalDateTime.now());

        return predictionRepository.save(prediction);
    }

    /**
     * 특정 사용자의 예측 내역 전체 조회
     */
    public List<PredictionEntity> getUserPredictions(Long userId) {
        return predictionRepository.findByUserId(userId);
    }

    /**
     * 단건 예측 결과 직접 업데이트 (기존 유지)
     */
    public PredictionEntity updatePredictionResult(Long predictionId, boolean isCorrect) {

        PredictionEntity prediction =
                predictionRepository.findByPredictionId(predictionId);

        if (prediction == null) {
            throw new RuntimeException("예측 데이터가 존재하지 않습니다.");
        }

        prediction.setIsCorrect(isCorrect);

        return predictionRepository.save(prediction);
    }

    /**
     * 💡 [추가] 경기 종료 후 해당 경기의 모든 사용자 예측 데이터를 일괄 정산합니다.
     */
    @Transactional
    public void settlePredictionsForMatch(MatchEntity match) {
        // 해당 경기 ID로 참여한 모든 유저의 예측 데이터 가져오기
        List<PredictionEntity> predictions = predictionRepository.findByMatchId(match.getMatchId());

        if (predictions.isEmpty()) {
            return; // 투표한 사용자가 없으면 바로 종료
        }

        // 실제 경기 스코어 비교 후 결과 문자열 도출 ('home' | 'draw' | 'away')
        String actualResult;
        if (match.getHomeScore() > match.getAwayScore()) {
            actualResult = "home";
        } else if (match.getHomeScore() < match.getAwayScore()) {
            actualResult = "away";
        } else {
            actualResult = "draw";
        }

        // 유저들의 예측값과 실제 결과를 비교하여 성공(true)/실패(false)로 정산 변경
        for (PredictionEntity prediction : predictions) {
            boolean isCorrect = prediction.getPredictedResult().equals(actualResult);
            prediction.setIsCorrect(isCorrect);
        }

        // 변경된 정산 결과를 DB에 일괄 저장
        predictionRepository.saveAll(predictions);
    }

    public List<PredictionEntity> getAllPredictions() {
        return predictionRepository.findAll();
    }

    public PredictionEntity getPrediction(Long predictionId) {

        PredictionEntity prediction =
                predictionRepository.findByPredictionId(predictionId);

        if (prediction == null) {
            throw new RuntimeException("예측 데이터가 존재하지 않습니다.");
        }

        return prediction;
    }
    public List<PredictionRankingDto> getPredictionRanking() {

        List<Object[]> results = predictionRepository.getPredictionRanking();

        List<PredictionRankingDto> rankingList = new ArrayList<>();

        for (Object[] row : results) {

            Long userId = ((Number) row[0]).longValue();
            Long totalPredictions = ((Number) row[1]).longValue();
            Long correctPredictions = ((Number) row[2]).longValue();
            Double accuracy = ((Number) row[3]).doubleValue();

            PredictionRankingDto dto = new PredictionRankingDto(
                    userId,
                    totalPredictions,
                    correctPredictions,
                    accuracy
            );

            rankingList.add(dto);
        }

        return rankingList;
    } //예측랭킹 매서드

}
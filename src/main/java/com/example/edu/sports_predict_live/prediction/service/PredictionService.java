package com.example.edu.sports_predict_live.prediction.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.prediction.dto.request.PredictionRequestDTO;
import com.example.edu.sports_predict_live.prediction.dto.response.PredictionResponseDTO;
import com.example.edu.sports_predict_live.prediction.dto.response.RankingResponseDTO;
import com.example.edu.sports_predict_live.prediction.entity.Prediction;
import com.example.edu.sports_predict_live.prediction.repository.PredictionRepository;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    @Transactional
    public PredictionResponseDTO predict(Long userId, PredictionRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateResult(dto.getSportCode(), dto.getPredictedResult());

        Prediction prediction;

        if ("lol".equals(dto.getSportCode())) {
            // LOL — 기존 예측 있으면 수정, 없으면 신규 등록
            Optional<Prediction> existing =
                    predictionRepository.findByUser_UserIdAndLolMatchId(userId, dto.getLolMatchId());
            if (existing.isPresent()) {
                existing.get().updateResult(dto.getPredictedResult());
                return new PredictionResponseDTO(existing.get());
            }
            prediction = Prediction.builder()
                    .user(user)
                    .lolMatchId(dto.getLolMatchId())
                    .lolScheduledDate(dto.getLolScheduledDate())
                    .sportCode("lol")
                    .predictedResult(dto.getPredictedResult())
                    .build();
        } else {
            // KBO / K리그 — 경기 시작 전 검증
            Match match = matchRepository.findById(dto.getMatchId())
                    .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

            if (!match.getStatus().equals("scheduled")) {
                throw new CustomException(ErrorCode.MATCH_ALREADY_STARTED);
            }
            if (match.getScheduledAt().isBefore(LocalDateTime.now())) {
                throw new CustomException(ErrorCode.MATCH_ALREADY_STARTED);
            }

            // 기존 예측 있으면 수정, 없으면 신규 등록
            Optional<Prediction> existing =
                    predictionRepository.findByUser_UserIdAndMatch_MatchId(userId, dto.getMatchId());
            if (existing.isPresent()) {
                existing.get().updateResult(dto.getPredictedResult());
                return new PredictionResponseDTO(existing.get());
            }

            prediction = Prediction.builder()
                    .user(user)
                    .match(match)
                    .sportCode(dto.getSportCode())
                    .predictedResult(dto.getPredictedResult())
                    .build();
        }

        return new PredictionResponseDTO(predictionRepository.save(prediction));
    }

    @Transactional(readOnly = true)
    public Optional<PredictionResponseDTO> getMyPrediction(Long userId, Long matchId) {
        return predictionRepository.findByUser_UserIdAndMatch_MatchId(userId, matchId)
                .map(PredictionResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public Optional<PredictionResponseDTO> getMyLolPrediction(Long userId, String lolMatchId) {
        return predictionRepository.findByUser_UserIdAndLolMatchId(userId, lolMatchId)
                .map(PredictionResponseDTO::new);
    }

    @Transactional(readOnly = true)
    public List<PredictionResponseDTO> getMyPredictions(Long userId) {
        return predictionRepository.findByUserIdWithMatchFetch(userId)
                .stream()
                .map(PredictionResponseDTO::new)
                .toList();
    }

    @Cacheable(value = "ranking", key = "#sportCode != null ? #sportCode : 'all'")
    @Transactional(readOnly = true)
    public List<RankingResponseDTO> getRanking(String sportCode) {
        List<Object[]> rows = predictionRepository.findRanking(
                (sportCode == null || sportCode.isBlank()) ? null : sportCode,
                PageRequest.of(0, 100)
        );
        List<RankingResponseDTO> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            result.add(new RankingResponseDTO(i + 1, rows.get(i)));
        }
        return result;
    }

    // KBO·K리그 완료 경기 정산 (스케줄러 호출)
    @Transactional
    public void settleDbMatches(String sportCode) {
        List<Prediction> unsettled = predictionRepository.findUnsettledDbPredictions(sportCode);
        for (Prediction p : unsettled) {
            Match m = p.getMatch();
            String actual = calcDbResult(m.getHomeScore(), m.getAwayScore(), sportCode);
            p.settle(actual);
        }
    }

    // 취소된 KBO·K리그 경기의 예측 무효화 (스케줄러 호출)
    @Transactional
    public void voidCancelledDbMatches() {
        List<Prediction> cancelled = predictionRepository.findCancelledDbPredictions();
        for (Prediction p : cancelled) {
            p.voidByCancellation();
        }
    }

    private String calcDbResult(int homeScore, int awayScore, String sportCode) {
        if (homeScore > awayScore) return "HOME_WIN";
        if (homeScore < awayScore) return "AWAY_WIN";
        return "DRAW"; // 야구/축구 무승부
    }

    private void validateResult(String sportCode, String result) {
        if ("lol".equals(sportCode) && "DRAW".equals(result)) {
            throw new CustomException(ErrorCode.INVALID_PREDICTION_RESULT);
        }
        if (!List.of("HOME_WIN", "DRAW", "AWAY_WIN").contains(result)) {
            throw new CustomException(ErrorCode.INVALID_PREDICTION_RESULT);
        }
    }
}

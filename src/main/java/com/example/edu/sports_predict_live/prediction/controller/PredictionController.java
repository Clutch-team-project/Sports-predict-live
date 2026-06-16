package com.example.edu.sports_predict_live.prediction.controller;

import com.example.edu.sports_predict_live.prediction.dto.request.PredictionRequestDTO;
import com.example.edu.sports_predict_live.prediction.dto.response.PredictionResponseDTO;
import com.example.edu.sports_predict_live.prediction.dto.response.RankingResponseDTO;
import com.example.edu.sports_predict_live.prediction.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    // 예측 제출
    @PostMapping
    public ResponseEntity<PredictionResponseDTO> predict(
            @AuthenticationPrincipal Long userId,
            @RequestBody PredictionRequestDTO dto) {
        return ResponseEntity.ok(predictionService.predict(userId, dto));
    }

    // 내 예측 조회 — KBO·K리그
    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getMyPrediction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long matchId) {
        return predictionService.getMyPrediction(userId, matchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // 내 예측 조회 — LOL
    @GetMapping("/lol/{lolMatchId}")
    public ResponseEntity<?> getMyLolPrediction(
            @AuthenticationPrincipal Long userId,
            @PathVariable String lolMatchId) {
        return predictionService.getMyLolPrediction(userId, lolMatchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // 내 예측 기록 전체
    @GetMapping("/me")
    public ResponseEntity<List<PredictionResponseDTO>> getMyPredictions(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(predictionService.getMyPredictions(userId));
    }

    // 포인트 순위 — ?sport=baseball|soccer|lol|전체(생략)
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingResponseDTO>> getRanking(
            @RequestParam(required = false) String sport) {
        return ResponseEntity.ok(predictionService.getRanking(sport));
    }
}

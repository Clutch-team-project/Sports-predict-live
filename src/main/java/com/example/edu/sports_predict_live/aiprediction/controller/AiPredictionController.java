package com.example.edu.sports_predict_live.aiprediction.controller;

import com.example.edu.sports_predict_live.aiprediction.dto.AiPredictionResponseDTO;
import com.example.edu.sports_predict_live.aiprediction.service.AiPredictionService;
import com.example.edu.sports_predict_live.prediction.dto.response.PredictionStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-prediction")
@RequiredArgsConstructor
public class AiPredictionController {

    private final AiPredictionService aiPredictionService;

    // 홈 통계 카드용: AI 예측 건수 & 적중률
    @GetMapping("/stats")
    public ResponseEntity<PredictionStatsDTO> getAiStats() {
        return ResponseEntity.ok(aiPredictionService.getAiStats());
    }

    // 야구
    @GetMapping("/baseball/{matchId}")
    public ResponseEntity<AiPredictionResponseDTO> baseball(@PathVariable Long matchId) {
        return ResponseEntity.ok(aiPredictionService.predictBaseball(matchId));
    }

    // 하위 호환 — 기존 /api/ai-prediction/{matchId} 유지
    @GetMapping("/{matchId}")
    public ResponseEntity<AiPredictionResponseDTO> baseballLegacy(@PathVariable Long matchId) {
        return ResponseEntity.ok(aiPredictionService.predictBaseball(matchId));
    }

    // 축구
    @GetMapping("/soccer/{matchId}")
    public ResponseEntity<AiPredictionResponseDTO> soccer(@PathVariable Long matchId) {
        return ResponseEntity.ok(aiPredictionService.predictSoccer(matchId));
    }

    // LoL — matchId는 lolesports 문자열 ID
    @GetMapping("/lol")
    public ResponseEntity<AiPredictionResponseDTO> lol(
            @RequestParam String matchId,
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam(defaultValue = "2025") String season) {
        return ResponseEntity.ok(aiPredictionService.predictLol(matchId, homeTeam, awayTeam, season));
    }
}

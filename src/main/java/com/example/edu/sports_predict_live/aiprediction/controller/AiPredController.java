package com.example.edu.sports_predict_live.aiprediction.controller;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.service.AiPredService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai-prediction")
@RequiredArgsConstructor
public class AiPredController {

    private final AiPredService aiPredService;

    /**
     * 특정 경기 ID만 받아 백엔드 내부 통계 데이터 기반으로 AI 승률을 계산하고 저장합니다.
     * 외부에서 파라미터로 확률을 구해서 던져줄 필요가 없어졌습니다.
     */
    @PostMapping("/save")
    public AiPredEntity saveAiPrediction(@RequestParam Long matchId) {
        // 기존 saveAiPrediction 대신 내부 연산 기능이 추가된 메서드 호출
        return aiPredService.calculateAndSaveAiPrediction(matchId);
    }

    /**
     * 특정 경기의 AI 예측 결과 및 근거 조회
     */
    @GetMapping("/{matchId}")
    public AiPredEntity getAiPrediction(@PathVariable Long matchId) {
        return aiPredService.getAiPrediction(matchId);
    }

    @GetMapping("/latest")
    public AiPredEntity getLatestPrediction() {

        return aiPredService.getLatestPrediction();
    } //메인 화면용 최신 ai 예측 조회
}
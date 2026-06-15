package com.example.edu.sports_predict_live.prediction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DbSettlementService {

    private final PredictionService predictionService;

    // 1분마다 KBO·K리그 완료 경기 정산
    @Scheduled(fixedDelay = 60_000)
    public void settle() {
        try {
            predictionService.settleDbMatches("baseball");
            predictionService.settleDbMatches("soccer");
        } catch (Exception e) {
            log.warn("DB 경기 정산 중 오류: {}", e.getMessage());
        }
    }
}

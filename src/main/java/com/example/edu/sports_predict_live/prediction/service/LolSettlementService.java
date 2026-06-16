package com.example.edu.sports_predict_live.prediction.service;

import com.example.edu.sports_predict_live.match.service.ScheduleService;
import com.example.edu.sports_predict_live.prediction.entity.Prediction;
import com.example.edu.sports_predict_live.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LolSettlementService {

    private final PredictionRepository predictionRepository;
    private final ScheduleService scheduleService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 5분마다 미정산 LOL 예측 자동 정산
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void settleLolPredictions() {
        List<Prediction> unsettled = predictionRepository.findBySportCodeAndIsCorrectIsNullAndActualResultIsNull("lol");
        if (unsettled.isEmpty()) return;

        // 날짜별로 그룹화
        Set<LocalDate> dates = unsettled.stream()
                .map(Prediction::getLolScheduledDate)
                .filter(d -> d != null)
                .collect(Collectors.toSet());

        for (LocalDate date : dates) {
            try {
                List<Map<String, Object>> matches =
                        scheduleService.getLolSchedule(date.format(DATE_FMT));

                // 완료된 경기만 추출 (matchId → 결과)
                Map<String, String> finishedResults = matches.stream()
                        .filter(m -> "finished".equals(m.get("status")))
                        .collect(Collectors.toMap(
                                m -> String.valueOf(m.get("matchId")),
                                m -> calcLolResult((int) m.get("homeWins"), (int) m.get("awayWins"))
                        ));

                // 취소된 경기 matchId 목록 — 예측 무효화 대상
                Set<String> cancelledIds = matches.stream()
                        .filter(m -> "cancelled".equals(m.get("status")))
                        .map(m -> String.valueOf(m.get("matchId")))
                        .collect(Collectors.toSet());

                if (finishedResults.isEmpty() && cancelledIds.isEmpty()) continue;

                // 해당 날짜 미정산 예측 정산
                List<Prediction> todayPredictions =
                        predictionRepository.findBySportCodeAndLolScheduledDateAndIsCorrectIsNullAndActualResultIsNull("lol", date);

                for (Prediction p : todayPredictions) {
                    if (cancelledIds.contains(p.getLolMatchId())) {
                        p.voidByCancellation();
                        log.debug("LOL 예측 무효화(경기 취소): predictionId={}", p.getPredictionId());
                        continue;
                    }
                    String result = finishedResults.get(p.getLolMatchId());
                    if (result != null) {
                        p.settle(result);
                        log.debug("LOL 예측 정산: predictionId={}, result={}", p.getPredictionId(), result);
                    }
                }
            } catch (Exception e) {
                log.warn("LOL 정산 중 오류 발생 (date={}): {}", date, e.getMessage());
            }
        }
    }

    private String calcLolResult(int homeWins, int awayWins) {
        return homeWins > awayWins ? "HOME_WIN" : "AWAY_WIN";
    }
}

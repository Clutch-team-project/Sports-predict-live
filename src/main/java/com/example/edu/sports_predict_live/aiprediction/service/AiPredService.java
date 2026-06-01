package com.example.edu.sports_predict_live.aiprediction.service;

import com.example.edu.sports_predict_live.aiprediction.entity.AiPredEntity;
import com.example.edu.sports_predict_live.aiprediction.entity.MatchEntity; // 💡 엔티티 임포트 추가
import com.example.edu.sports_predict_live.aiprediction.repository.AiPredRepository;
import com.example.edu.sports_predict_live.aiprediction.repository.MatchRepository;     // 💡 매치 레포지토리 임포트 추가
import com.example.edu.sports_predict_live.aiprediction.repository.MatchTeamStatRepository; // 💡 통계 레포지토리 임포트 추가
import com.example.edu.sports_predict_live.aiprediction.dto.AiPredBasisDto;
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

    private final AiPredRepository aiPredRepository;
    private final ObjectMapper objectMapper;
    private final MatchRepository matchRepository;
    private final MatchTeamStatRepository matchTeamStatRepository;

    @Transactional
    public AiPredEntity calculateAndSaveAiPrediction(Long matchId) {

        // 1. 경기당 AI 예측 1개만 허용 (중복 검증)
        boolean exists = aiPredRepository.existsByMatchId(matchId);
        if (exists) {
            throw new RuntimeException("이미 AI 예측이 존재합니다.");
        }

        // 2. Match 테이블에서 경기 정보 호출
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 경기입니다."));

        // 🛠️ [getSeason() 오류 해결]
        // 팀원의 MatchEntity에 season 필드가 없으므로, 변수를 직접 꺼내지 않고 임시 문자열로 대체합니다.
        // 나중에 팀원이 MatchEntity에 season 필드를 추가하면 match.getSeason()으로 교체하면 됩니다.
        String currentSeason = "2026";

        // 3. 경기 통계 데이터를 활용한 가중치 계산 (현재 MatchEntity에 맞게 매핑)
        Double homeSeasonScore = matchTeamStatRepository.findAverageStatByTeam(match.getHomeTeamId(), currentSeason);
        Double awaySeasonScore = matchTeamStatRepository.findAverageStatByTeam(match.getAwayTeamId(), currentSeason);

        Double headToHeadScore = 0.15;
        Double homeAdvantage = 0.10;

        // 가중치 합산
        double homeWeight = 1.0 + homeSeasonScore + headToHeadScore + homeAdvantage;
        double awayWeight = 1.0 + awaySeasonScore;
        double drawWeight = 0.7;

        double totalWeight = homeWeight + drawWeight + awayWeight;

        // 4. 부동소수점 오차 방지 및 앱 레벨 합산 1.0 강제 정규화
        BigDecimal homeProb = BigDecimal.valueOf(homeWeight / totalWeight).setScale(4, RoundingMode.HALF_UP);
        BigDecimal drawProb = BigDecimal.valueOf(drawWeight / totalWeight).setScale(4, RoundingMode.HALF_UP);
        BigDecimal awayProb = BigDecimal.ONE.subtract(homeProb).subtract(drawProb);

        // 5. basis JSON 데이터 생성
        String basisJson;
        try {
            AiPredBasisDto basisDto = new AiPredBasisDto();
            basisDto.setHome_season(homeSeasonScore);
            basisDto.setAway_season(awaySeasonScore);
            basisDto.setHead_to_head(headToHeadScore);
            basisDto.setHome_advantage(homeAdvantage);

            basisJson = objectMapper.writeValueAsString(basisDto);
        } catch (Exception e) {
            basisJson = "{}";
        }

        // 6. 엔티티 생성 및 저장
        AiPredEntity aiPred = new AiPredEntity();
        aiPred.setMatchId(matchId);
        aiPred.setHomeWinProb(homeProb.doubleValue());
        aiPred.setDrawProb(drawProb.doubleValue());
        aiPred.setAwayWinProb(awayProb.doubleValue());
        aiPred.setBasis(basisJson);
        aiPred.setCreatedAt(LocalDateTime.now());

        return aiPredRepository.save(aiPred);
    }

    public AiPredEntity getAiPrediction(Long matchId) {
        return aiPredRepository.findByMatchId(matchId);
    }
}
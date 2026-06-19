package com.example.edu.sports_predict_live.aiprediction.entity;

import com.example.edu.sports_predict_live.match.entity.Match;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "ai_pred",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ai_pred_match",     columnNames = "match_id"),
        @UniqueConstraint(name = "uk_ai_pred_lol_match", columnNames = "lol_match_id")
    }
)
@Getter
@NoArgsConstructor
public class AiPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_pred_id")
    private Long aiPredId;

    // KBO·K리그 경기 FK (LoL은 NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    // LoL 경기 ID 문자열 (KBO·K리그는 NULL)
    @Column(name = "lol_match_id", length = 100)
    private String lolMatchId;

    @Column(name = "sport_code", nullable = false, length = 20)
    private String sportCode;

    @Column(name = "home_win_prob", nullable = false, precision = 5, scale = 4)
    private BigDecimal homeWinProb;

    @Column(name = "draw_prob", nullable = false, precision = 5, scale = 4)
    private BigDecimal drawProb;

    @Column(name = "away_win_prob", nullable = false, precision = 5, scale = 4)
    private BigDecimal awayWinProb;

    // AI 예측 근거 JSON
    @Column(name = "basis", columnDefinition = "JSON")
    private String basis;

    @Column(name = "reasoning", columnDefinition = "TEXT")
    private String reasoning;

    @Column(name = "key_factors", columnDefinition = "JSON")
    private String keyFactors;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiPrediction(Match match, String lolMatchId, String sportCode,
                        BigDecimal homeWinProb, BigDecimal drawProb,
                        BigDecimal awayWinProb, String basis,
                        String reasoning, String keyFactors) {
        this.match       = match;
        this.lolMatchId  = lolMatchId;
        this.sportCode   = sportCode;
        this.homeWinProb = homeWinProb;
        this.drawProb    = drawProb;
        this.awayWinProb = awayWinProb;
        this.basis       = basis;
        this.reasoning   = reasoning;
        this.keyFactors  = keyFactors;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

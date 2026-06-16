package com.example.edu.sports_predict_live.prediction.entity;

import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "match_id"}),
        @UniqueConstraint(columnNames = {"user_id", "lol_match_id"})
})
@Getter
@NoArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long predictionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // KBO·K리그 경기 (null for LOL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    // lolesports 외부 API 경기 ID (null for KBO/Soccer)
    @Column(name = "lol_match_id", length = 50)
    private String lolMatchId;

    // 자동 정산 시 해당 날짜로 lolesports API 재조회
    @Column(name = "lol_scheduled_date")
    private LocalDate lolScheduledDate;

    @Column(name = "sport_code", nullable = false, length = 20)
    private String sportCode; // baseball / soccer / lol

    // HOME_WIN / DRAW / AWAY_WIN
    @Column(name = "predicted_result", nullable = false, length = 20)
    private String predictedResult;

    // 경기 종료 후 채움 — null이면 미정산
    @Column(name = "actual_result", length = 20)
    private String actualResult;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Prediction(User user, Match match, String lolMatchId, LocalDate lolScheduledDate,
                      String sportCode, String predictedResult) {
        this.user = user;
        this.match = match;
        this.lolMatchId = lolMatchId;
        this.lolScheduledDate = lolScheduledDate;
        this.sportCode = sportCode;
        this.predictedResult = predictedResult;
    }

    public void updateResult(String newPredictedResult) {
        this.predictedResult = newPredictedResult;
    }

    public void settle(String actualResult) {
        this.actualResult = actualResult;
        this.isCorrect = this.predictedResult.equals(actualResult);
        this.pointsEarned = Boolean.TRUE.equals(this.isCorrect) ? 100 : 0;
    }
}

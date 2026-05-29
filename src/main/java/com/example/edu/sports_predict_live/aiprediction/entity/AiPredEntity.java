package com.example.edu.sports_predict_live.aiprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_pred")
@Getter
@Setter
public class AiPredEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_pred_id")
    private Long aiPredId;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "home_win_prob")
    private Double homeWinProb;

    @Column(name = "draw_prob")
    private Double drawProb;

    @Column(name = "away_win_prob")
    private Double awayWinProb;

    @Column(name = "basis")
    private String basis;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
package com.example.edu.sports_predict_live.aiprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction")
@Getter
@Setter
public class PredictionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long predictionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "predicted_result")
    private String predictedResult;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
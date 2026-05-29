package com.example.edu.sports_predict_live.aiprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "`match`")
@Getter
@Setter
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "sport_id")
    private Long sportId;

    @Column(name = "home_team_id")
    private Long homeTeamId;

    @Column(name = "away_team_id")
    private Long awayTeamId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "status")
    private String status;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "venue")
    private String venue;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} //경기 테이블 호출 필요 현재 없는 상태로 오류메시지는 무시
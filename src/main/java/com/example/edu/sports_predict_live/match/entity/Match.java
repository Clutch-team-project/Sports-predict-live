package com.example.edu.sports_predict_live.match.entity;

import com.example.edu.sports_predict_live.sport.entity.Sport;
import com.example.edu.sports_predict_live.team.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 경기 일정/결과 — KBO·K리그 전용 (LOL 경기는 lolesports API 실시간 조회)
@Entity
@Table(name = "`match`")
@Getter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    // 'scheduled' | 'in_progress' | 'paused' | 'finished' | 'cancelled'
    @Column(name = "status", nullable = false, length = 20)
    private String status = "scheduled";

    @Column(name = "home_score", nullable = false)
    private int homeScore = 0;

    @Column(name = "away_score", nullable = false)
    private int awayScore = 0;

    @Column(name = "season", length = 20)
    private String season;

    @Column(name = "venue", length = 100)
    private String venue;

    @Column(name = "winning_pitcher", length = 50)
    private String winningPitcher;

    @Column(name = "losing_pitcher", length = 50)
    private String losingPitcher;

    @Column(name = "current_pitcher", length = 50)
    private String currentPitcher;

    @Column(name = "starting_pitcher_away", length = 50)
    private String startingPitcherAway;

    @Column(name = "starting_pitcher_home", length = 50)
    private String startingPitcherHome;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateScore(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}

package com.example.edu.sports_predict_live.team.entity;

import com.example.edu.sports_predict_live.sport.entity.Sport;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 팀 시즌 누적 성적 — 순위표 원본 데이터 (크롤러가 갱신)
@Entity
@Table(
        name = "team_season_stat",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_sport_season",
                columnNames = {"team_id", "sport_id", "season"}
        )
)
@Getter
@NoArgsConstructor
public class TeamSeasonStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_season_stat_id")
    private Long teamSeasonStatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @Column(name = "season", nullable = false, length = 20)
    private String season;

    @Column(name = "wins", nullable = false)
    private int wins = 0;

    @Column(name = "draws", nullable = false)
    private int draws = 0;

    @Column(name = "losses", nullable = false)
    private int losses = 0;

    @Column(name = "points_for", nullable = false)
    private int pointsFor = 0;

    @Column(name = "points_against", nullable = false)
    private int pointsAgainst = 0;

    @Column(name = "`rank`")
    private Integer rank;

    @Column(name = "win_rate", precision = 5, scale = 3)
    private BigDecimal winRate;

    // W·D·L 조합 예: 'WWDLW'
    @Column(name = "recent_form", length = 5)
    private String recentForm;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
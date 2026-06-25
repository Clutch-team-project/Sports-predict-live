package com.example.edu.sports_predict_live.livematch.stat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_team_stat")
@Getter
@NoArgsConstructor
public class MatchTeamStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_team_stat_id")
    private Long matchTeamStatId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "stat_key", nullable = false, length = 50)
    private String statKey;

    @Column(name = "stat_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal statValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

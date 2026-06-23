package com.example.edu.sports_predict_live.livematch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_player_stat")
@Getter
@NoArgsConstructor
public class MatchPlayerStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_player_stat_id")
    private Long matchPlayerStatId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "stat_key", nullable = false, length = 50)
    private String statKey;

    @Column(name = "stat_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal statValue;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

package com.example.edu.sports_predict_live.player.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "player_season_stat_lol")
@Getter
@NoArgsConstructor
public class PlayerSeasonStatLol {

    @Id
    @Column(name = "player_season_stat_id")
    private Long playerSeasonStatId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "player_season_stat_id")
    private PlayerSeasonStat playerSeasonStat;

    // (킬+어시스트)/데스 — 데스=0이면 NULL
    @Column(name = "kda", precision = 6, scale = 2)
    private BigDecimal kda;

    @Column(name = "avg_kills", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgKills = BigDecimal.ZERO;

    @Column(name = "avg_deaths", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgDeaths = BigDecimal.ZERO;

    @Column(name = "avg_assists", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgAssists = BigDecimal.ZERO;

    @Column(name = "cs_per_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal csPerMin = BigDecimal.ZERO;

    // 0.0~1.0
    @Column(name = "win_rate", precision = 5, scale = 3)
    private BigDecimal winRate;
}

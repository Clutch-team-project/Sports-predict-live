package com.example.edu.sports_predict_live.player.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "player_season_stat_baseball")
@Getter
@NoArgsConstructor
public class PlayerSeasonStatBaseball {

    // player_season_stat과 1:1 — PK를 공유
    @Id
    @Column(name = "player_season_stat_id")
    private Long playerSeasonStatId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "player_season_stat_id")
    private PlayerSeasonStat playerSeasonStat;

    // 타자 전용 (투수는 NULL)
    @Column(name = "batting_avg", precision = 5, scale = 3)
    private BigDecimal battingAvg;

    @Column(name = "hits")
    private Integer hits;

    @Column(name = "home_runs")
    private Integer homeRuns;

    @Column(name = "rbi")
    private Integer rbi;

    // 투수 전용 (타자는 NULL)
    @Column(name = "era", precision = 5, scale = 2)
    private BigDecimal era;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "strikeouts")
    private Integer strikeouts;

    @Column(name = "losses")
    private Integer losses;

    @Column(name = "saves")
    private Integer saves;

    @Column(name = "holds")
    private Integer holds;

    // 규정타석/규정이닝 충족 여부 — true면 전체 랭킹에 노출, false면 팀 선택 시에만 노출
    @Column(name = "qualified")
    private Boolean qualified;
}

package com.example.edu.sports_predict_live.player.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "player_season_stat_soccer")
@Getter
@NoArgsConstructor
public class PlayerSeasonStatSoccer {

    @Id
    @Column(name = "player_season_stat_id")
    private Long playerSeasonStatId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "player_season_stat_id")
    private PlayerSeasonStat playerSeasonStat;

    @Column(name = "goals", nullable = false)
    private int goals = 0;

    @Column(name = "assists", nullable = false)
    private int assists = 0;

    @Column(name = "yellow_cards", nullable = false)
    private int yellowCards = 0;

    @Column(name = "red_cards", nullable = false)
    private int redCards = 0;

    // 골키퍼 전용 — 비해당 포지션은 0
    @Column(name = "clean_sheets", nullable = false)
    private int cleanSheets = 0;
}

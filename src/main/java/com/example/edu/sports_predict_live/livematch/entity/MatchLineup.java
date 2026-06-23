package com.example.edu.sports_predict_live.livematch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "match_lineup")
@Getter
@NoArgsConstructor
public class MatchLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_lineup_id")
    private Long matchLineupId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "is_starter", nullable = false)
    private boolean starter;

    @Column(name = "order_num")
    private Integer orderNum;

    @Column(name = "position", length = 30)
    private String position;
}
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

    public static MatchLineup create(Long matchId, Long teamId, Long playerId, boolean starter, Integer orderNum, String position) {
        MatchLineup lineup = new MatchLineup();
        lineup.matchId = matchId;
        lineup.teamId = teamId;
        lineup.playerId = playerId;
        lineup.starter = starter;
        lineup.orderNum = orderNum;
        lineup.position = position;
        return lineup;
    }

    public void update(boolean starter, Integer orderNum, String position) {
        this.starter = starter;
        this.orderNum = orderNum;
        this.position = position;
    }
}

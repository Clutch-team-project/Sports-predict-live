package com.example.edu.sports_predict_live.player.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "player_season_stat",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_player_season",
        columnNames = {"player_id", "season"}
    )
)
@Getter
@NoArgsConstructor
public class PlayerSeasonStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_season_stat_id")
    private Long playerSeasonStatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "season", nullable = false, length = 20)
    private String season;

    @Column(name = "games_played", nullable = false)
    private int gamesPlayed = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

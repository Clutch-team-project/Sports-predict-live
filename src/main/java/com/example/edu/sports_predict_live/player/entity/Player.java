package com.example.edu.sports_predict_live.player.entity;

import com.example.edu.sports_predict_live.team.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "player",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_source_external_player_id",
        columnNames = {"source", "external_player_id"}
    )
)
@Getter
@NoArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // 크롤링 출처 예: 'kbo' | 'kleague' | 'lol'
    @Column(name = "source", nullable = false, length = 50)
    private String source;

    // 출처 사이트의 고유 선수 ID
    @Column(name = "external_player_id", nullable = false, length = 100)
    private String externalPlayerId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "position", length = 30)
    private String position;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    // S3 object key
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    // LoL 선수 전용 — 야구/축구는 NULL
    @Column(name = "riot_puuid", length = 100)
    private String riotPuuid;

    @Column(name = "riot_game_name", length = 100)
    private String riotGameName;

    @Column(name = "riot_tag_line", length = 10)
    private String riotTagLine;
}

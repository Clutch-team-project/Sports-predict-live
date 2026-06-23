package com.example.edu.sports_predict_live.livematch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_event")
@Getter
@NoArgsConstructor
public class MatchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_event_id")
    private Long matchEventId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "event_time")
    private Integer eventTime;

    @Column(name = "event_period", length = 20)
    private String eventPeriod;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private MatchEvent(
            Long matchId,
            Long teamId,
            Long playerId,
            String eventType,
            String eventPeriod,
            Integer eventTime,
            String description
    ) {
        this.matchId = matchId;
        this.teamId = teamId;
        this.playerId = playerId;
        this.eventType = eventType;
        this.eventPeriod = eventPeriod;
        this.eventTime = eventTime;
        this.description = description;
    }

    public static MatchEvent create(
            Long matchId,
            Long teamId,
            Long playerId,
            String eventType,
            String eventPeriod,
            Integer eventTime,
            String description
    ) {
        return new MatchEvent(matchId, teamId, playerId, eventType, eventPeriod, eventTime, description);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
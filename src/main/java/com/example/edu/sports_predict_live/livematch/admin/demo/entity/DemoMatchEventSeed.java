package com.example.edu.sports_predict_live.livematch.admin.demo.entity;

import com.example.edu.sports_predict_live.livematch.event.entity.MatchEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "demo_match_event_seed",
        indexes = {
                @Index(name = "idx_demo_seed_match_delay", columnList = "match_id, published, delay_seconds, seq_no"),
                @Index(name = "idx_demo_seed_match_seq", columnList = "match_id, seq_no")
        })
@Getter
@NoArgsConstructor
public class DemoMatchEventSeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seed_id")
    private Long seedId;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "seq_no", nullable = false)
    private Integer seqNo;

    @Column(name = "delay_seconds", nullable = false)
    private Integer delaySeconds = 4;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "event_period", length = 20)
    private String eventPeriod;

    @Column(name = "event_time")
    private Integer eventTime;

    @Column(name = "description")
    private String description;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MatchEvent toMatchEvent() {
        Integer resolvedEventTime = eventTime != null ? eventTime : seqNo;
        return MatchEvent.create(matchId, teamId, playerId, eventType, eventPeriod, resolvedEventTime, description);
    }

    public void markPublished(LocalDateTime publishedAt) {
        this.published = true;
        this.publishedAt = publishedAt;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (delaySeconds == null) delaySeconds = 4;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

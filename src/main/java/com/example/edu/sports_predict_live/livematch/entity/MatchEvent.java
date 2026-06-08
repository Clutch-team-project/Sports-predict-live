package com.example.edu.sports_predict_live.livematch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_event")
@Getter
@NoArgsConstructor
public class MatchEvent {

    /*
     * match_event 테이블과 매핑되는 Entity.
     * DB의 한 행(row)이 Java에서는 MatchEvent 객체 하나로 표현된다.
     *
     * 현재 역할:
     * - /api/games/{gameId}/events 조회 시 DB에서 읽어오는 대상
     *
     * 아직 역할이 아닌 것:
     * - 관리자 이벤트 저장
     * - WebSocket 실시간 전송
     * 이 기능들은 다음 Issue에서 별도로 붙인다.
     */

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

    /*
     * 새 MatchEvent를 DB에 저장하기 직전에 createdAt이 비어 있으면 현재 시간으로 채운다.
     * 조회 API만 쓸 때는 거의 실행되지 않고, 나중에 관리자 이벤트 저장 API에서 의미가 생긴다.
     */
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

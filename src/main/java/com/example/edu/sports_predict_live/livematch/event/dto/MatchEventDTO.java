package com.example.edu.sports_predict_live.livematch.event.dto;

import com.example.edu.sports_predict_live.livematch.event.entity.MatchEvent;

import java.time.LocalDateTime;

public record MatchEventDTO(
        Long matchEventId,
        Long matchId,
        Long teamId,
        Long playerId,
        String eventType,
        String eventPeriod,
        Integer eventTime,
        String description,
        LocalDateTime createdAt
) {
    public static MatchEventDTO from(MatchEvent matchEvent) {
        return new MatchEventDTO(
                matchEvent.getMatchEventId(),
                matchEvent.getMatchId(),
                matchEvent.getTeamId(),
                matchEvent.getPlayerId(),
                matchEvent.getEventType(),
                matchEvent.getEventPeriod(),
                matchEvent.getEventTime(),
                matchEvent.getDescription(),
                matchEvent.getCreatedAt()
        );
    }
}
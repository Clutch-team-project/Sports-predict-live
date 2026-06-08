package com.example.edu.sports_predict_live.livematch.dto;

import com.example.edu.sports_predict_live.livematch.entity.MatchEvent;

import java.time.LocalDateTime;

public record MatchEventDTO(
        String eventType,
        Integer eventTime,
        String eventPeriod,
        Long playerId,
        Long teamId,
        String description,
        LocalDateTime createdAt
) {

    /*
     * Entity -> DTO 변환.
     *
     * Repository는 DB 테이블과 연결된 MatchEvent Entity를 반환한다.
     * Controller 응답에서는 Entity를 그대로 노출하지 않고, 필요한 값만 MatchEventDTO로 바꿔서 JSON으로 내보낸다.
     */
    public static MatchEventDTO from(MatchEvent matchEvent) {
        return new MatchEventDTO(
                matchEvent.getEventType(),
                matchEvent.getEventTime(),
                matchEvent.getEventPeriod(),
                matchEvent.getPlayerId(),
                matchEvent.getTeamId(),
                matchEvent.getDescription(),
                matchEvent.getCreatedAt()
        );
    }
}

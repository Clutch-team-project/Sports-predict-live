package com.example.edu.sports_predict_live.livematch.repository;

import com.example.edu.sports_predict_live.livematch.entity.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {

    /*
     * Spring Data JPA 메서드 이름 기반 조회.
     *
     * findByMatchId:
     * - match_event.match_id = ? 조건으로 조회한다.
     *
     * OrderByEventTimeAscMatchEventIdAsc:
     * - event_time 오름차순
     * - 같은 event_time이면 match_event_id 오름차순
     *
     * prompt/docs 기준의 정렬:
     * ORDER BY event_time ASC, match_event_id ASC
     */
    List<MatchEvent> findByMatchIdOrderByEventTimeAscMatchEventIdAsc(Long matchId);

    long countByMatchId(Long matchId);

    void deleteByMatchId(Long matchId);

    Optional<MatchEvent> findTopByMatchIdOrderByEventTimeDescMatchEventIdDesc(Long matchId);

    Optional<MatchEvent> findTopByMatchIdAndEventPeriodOrderByEventTimeDescMatchEventIdDesc(Long matchId, String eventPeriod);
}

package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchEventDTO;
import com.example.edu.sports_predict_live.livematch.entity.MatchEvent;
import com.example.edu.sports_predict_live.livematch.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchEventService {

    private static final Set<String> BASEBALL_EVENT_TYPES = Set.of(
            "pitch", "ball", "pitch_clock_ball", "called_strike", "swinging_strike", "check_swing_strike",
            "pitch_clock_strike", "foul", "foul_tip", "bunt_foul", "hit_by_pitch",
            "at_bat_start", "plate_appearance_result",
            "single", "double", "triple", "homerun", "walk", "intentional_walk",
            "error", "field_error", "defensive_error", "fielders_choice", "fielder_choice",
            "strikeout", "called_strikeout", "swinging_strikeout", "foul_tip_strikeout",
            "bunt_foul_strikeout", "pitch_clock_strikeout", "dropped_third_strike_safe",
            "dropped_third_strike_out", "groundout", "flyout", "lineout", "popout", "sac_bunt", "sac_fly",
            "double_play", "triple_play", "force_out", "tag_out",
            "stolen_base", "caught_stealing", "pickoff", "pickoff_throw", "pickoff_out",
            "runner_out", "defensive_indifference", "wild_pitch", "passed_ball", "balk", "runner_advance",
            "score", "inning_start", "inning_end", "game_start", "game_end",
            "pitcher_change", "batter_change", "defensive_substitution", "pinch_hitter", "pinch_runner",
            "review", "delay", "resume", "note"
    );

    private final MatchEventRepository matchEventRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<MatchEventDTO> getMatchEvents(Long matchId) {
        return matchEventRepository.findByMatchIdOrderByEventTimeAscMatchEventIdAsc(matchId).stream()
                .map(MatchEventDTO::from)
                .toList();
    }

    @Transactional
    public MatchEventDTO saveAdminEvent(Long matchId, MatchEventDTO request) {
        if (!matchRepository.existsById(matchId)) {
            throw new IllegalArgumentException("match not found: " + matchId);
        }

        String eventType = normalizeEventType(request.eventType());

        MatchEvent saved = matchEventRepository.save(MatchEvent.create(
                matchId,
                request.teamId(),
                request.playerId(),
                eventType,
                request.eventPeriod(),
                request.eventTime(),
                request.description()
        ));

        return MatchEventDTO.from(saved);
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }

        String normalized = eventType.trim().toLowerCase();

        if (!BASEBALL_EVENT_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("unsupported eventType: " + eventType);
        }

        return normalized;
    }
}

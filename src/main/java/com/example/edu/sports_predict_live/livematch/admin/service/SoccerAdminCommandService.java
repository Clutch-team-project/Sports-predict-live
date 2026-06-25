package com.example.edu.sports_predict_live.livematch.admin.service;

import com.example.edu.sports_predict_live.livematch.admin.dto.SoccerAdminAction;
import com.example.edu.sports_predict_live.livematch.admin.dto.SoccerAdminCommandDTO;
import com.example.edu.sports_predict_live.livematch.event.entity.MatchEvent;
import com.example.edu.sports_predict_live.livematch.event.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.livematch.soccer.dto.SoccerLiveDTO;
import com.example.edu.sports_predict_live.livematch.soccer.service.SoccerLiveStateService;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SoccerAdminCommandService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final SoccerLiveStateService soccerLiveStateService;

    @Transactional
    public SoccerLiveDTO apply(Long matchId, SoccerAdminCommandDTO command) {
        if (command == null || command.action() == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (command.action() == SoccerAdminAction.CLEAR_EVENTS) {
            return clearEvents(matchId);
        }
        if (command.action() == SoccerAdminAction.UNDO_LAST_EVENT) {
            return undoLastEvent(matchId);
        }

        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        EventWriter writer = new EventWriter(matchId);

        switch (command.action()) {
            case START_MATCH -> {
                match.updateStatus("live");
                if (matchEventRepository.countByMatchId(matchId) == 0) {
                    writer.add("first_half", null, null, "match_start", 0, text(command, "Kickoff"));
                }
            }
            case END_FIRST_HALF -> writer.add("first_half", null, null, "first_half_end", minute(command, 45), text(command, "Half time"));
            case START_SECOND_HALF -> writer.add("second_half", null, null, "second_half_start", minute(command, 46), text(command, "Second half starts"));
            case END_MATCH -> {
                writer.add("second_half", null, null, "match_end", minute(command, 90), text(command, "Full time"));
                match.updateStatus("finished");
            }
            case GOAL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "goal", command.normalizedMinute(), text(command, "Goal"));
            case OWN_GOAL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "own_goal", command.normalizedMinute(), text(command, "Own goal"));
            case PENALTY_GOAL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "penalty_goal", command.normalizedMinute(), text(command, "Penalty goal"));
            case PENALTY_MISS -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "penalty_miss", command.normalizedMinute(), text(command, "Penalty missed"));
            case ASSIST -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "assist", command.normalizedMinute(), text(command, "Assist"));
            case PASS -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "pass", command.normalizedMinute(), text(command, "Pass"));
            case SHOT -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "shot", command.normalizedMinute(), text(command, "Shot"));
            case SHOT_ON_TARGET -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "shot_on_target", command.normalizedMinute(), text(command, "Shot on target"));
            case CORNER_KICK -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "corner_kick", command.normalizedMinute(), text(command, "Corner kick"));
            case FOUL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "foul", command.normalizedMinute(), text(command, "Foul"));
            case YELLOW_CARD -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "yellow_card", command.normalizedMinute(), text(command, "Yellow card"));
            case RED_CARD -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "red_card", command.normalizedMinute(), text(command, "Red card"));
            case OFFSIDE -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "offside", command.normalizedMinute(), text(command, "Offside"));
            case SAVE -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "save", command.normalizedMinute(), text(command, "Save"));
            case SUBSTITUTION -> writer.add(command.normalizedPeriod(), command.teamId(), command.inPlayerId(), "substitution", command.normalizedMinute(), substitutionText(command));
            default -> throw new IllegalArgumentException("unsupported action: " + command.action());
        }

        matchEventRepository.flush();
        SoccerLiveDTO updated = soccerLiveStateService.getSoccerLive(matchId);
        syncMatchScore(match, updated);
        return updated;
    }

    @Transactional
    public SoccerLiveDTO clearEvents(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        matchEventRepository.deleteByMatchId(matchId);
        match.updateScore(0, 0);
        match.updateStatus("scheduled");
        matchEventRepository.flush();
        return soccerLiveStateService.getSoccerLive(matchId);
    }

    @Transactional
    public SoccerLiveDTO undoLastEvent(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        matchEventRepository.findTopByMatchIdOrderByEventTimeDescMatchEventIdDesc(matchId)
                .ifPresent(matchEventRepository::delete);
        matchEventRepository.flush();
        if (matchEventRepository.countByMatchId(matchId) == 0) {
            match.updateScore(0, 0);
            match.updateStatus("scheduled");
            return soccerLiveStateService.getSoccerLive(matchId);
        }
        SoccerLiveDTO updated = soccerLiveStateService.getSoccerLive(matchId);
        syncMatchScore(match, updated);
        return updated;
    }

    private String text(SoccerAdminCommandDTO command, String fallback) {
        return command.description() == null || command.description().isBlank() ? fallback : command.description().trim();
    }

    private String substitutionText(SoccerAdminCommandDTO command) {
        if (command.description() != null && !command.description().isBlank()) return command.description().trim();
        return "Substitution in=" + command.inPlayerId() + ", out=" + command.outPlayerId();
    }

    private int minute(SoccerAdminCommandDTO command, int fallback) {
        return command.minute() == null ? fallback : Math.max(0, command.minute());
    }

    private void syncMatchScore(Match match, SoccerLiveDTO updated) {
        if (updated.scoreboard() != null) {
            match.updateScore(updated.scoreboard().homeScore(), updated.scoreboard().awayScore());
        }
        if (!"finished".equals(match.getStatus()) && !"paused".equals(match.getStatus())) {
            match.updateStatus("live");
        }
    }

    private class EventWriter {
        private final Long matchId;
        private final Map<String, Integer> nextTimes = new HashMap<>();

        private EventWriter(Long matchId) {
            this.matchId = matchId;
        }

        private void add(String period, Long teamId, Long playerId, String eventType, int minute, String description) {
            int eventTime = minute > 0 ? minute : nextTimes.compute(period, (key, current) -> {
                if (current != null) return current + 1;
                return matchEventRepository
                        .findTopByMatchIdAndEventPeriodOrderByEventTimeDescMatchEventIdDesc(matchId, period)
                        .map(MatchEvent::getEventTime)
                        .orElse(0) + 1;
            });
            matchEventRepository.save(MatchEvent.create(matchId, teamId, playerId, eventType, period, eventTime, description));
        }
    }
}

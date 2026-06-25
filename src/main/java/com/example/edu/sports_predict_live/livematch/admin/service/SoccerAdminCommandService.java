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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

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
                    writer.add("first_half", null, null, "match_start", 0, text(command, "경기가 시작되었습니다."));
                }
            }
            case END_FIRST_HALF -> writer.add("first_half", null, null, "first_half_end", minute(command, 45), text(command, "전반전이 종료되었습니다."));
            case START_SECOND_HALF -> writer.add("second_half", null, null, "second_half_start", minute(command, 46), text(command, "후반전이 시작되었습니다."));
            case END_MATCH -> {
                writer.add("second_half", null, null, "match_end", minute(command, 90), text(command, "경기가 종료되었습니다."));
                match.updateStatus("finished");
            }
            case GOAL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "goal", command.normalizedMinute(), text(command, "득점이 기록되었습니다."));
            case OWN_GOAL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "own_goal", command.normalizedMinute(), text(command, "자책골이 기록되었습니다."));
            case PENALTY_GOAL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "penalty_goal", command.normalizedMinute(), text(command, "페널티킥 득점이 기록되었습니다."));
            case PENALTY_MISS -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "penalty_miss", command.normalizedMinute(), text(command, "페널티킥이 무산되었습니다."));
            case SHOT -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "shot", command.normalizedMinute(), text(command, "슈팅을 시도했습니다."));
            case SHOT_ON_TARGET -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "shot_on_target", command.normalizedMinute(), text(command, "유효슈팅이 기록되었습니다."));
            case CORNER_KICK -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "corner_kick", command.normalizedMinute(), text(command, "코너킥을 얻었습니다."));
            case FOUL -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "foul", command.normalizedMinute(), text(command, "파울이 선언되었습니다."));
            case YELLOW_CARD -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "yellow_card", command.normalizedMinute(), text(command, "경고가 주어졌습니다."));
            case RED_CARD -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "red_card", command.normalizedMinute(), text(command, "퇴장이 선언되었습니다."));
            case OFFSIDE -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "offside", command.normalizedMinute(), text(command, "오프사이드가 선언되었습니다."));
            case SAVE -> writer.add(command.normalizedPeriod(), command.teamId(), command.playerId(), "save", command.normalizedMinute(), text(command, "골키퍼 선방이 기록되었습니다."));
            case SUBSTITUTION -> writer.add(command.normalizedPeriod(), command.teamId(), command.inPlayerId(), "substitution", command.normalizedMinute(), substitutionText(command));
            default -> throw new IllegalArgumentException("unsupported action: " + command.action());
        }

        matchEventRepository.flush();
        SoccerLiveDTO updated = soccerLiveStateService.getSoccerLive(matchId);
        syncMatchScore(match, updated);
        return publishLive(matchId, updated);
    }

    @Transactional
    public SoccerLiveDTO clearEvents(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        matchEventRepository.deleteByMatchId(matchId);
        match.updateScore(0, 0);
        match.updateStatus("scheduled");
        matchEventRepository.flush();
        return publishLive(matchId, soccerLiveStateService.getSoccerLive(matchId));
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
            return publishLive(matchId, soccerLiveStateService.getSoccerLive(matchId));
        }
        SoccerLiveDTO updated = soccerLiveStateService.getSoccerLive(matchId);
        syncMatchScore(match, updated);
        return publishLive(matchId, updated);
    }


    private SoccerLiveDTO publishLive(Long matchId, SoccerLiveDTO updated) {
        messagingTemplate.convertAndSend("/topic/games/" + matchId + "/soccer-live", updated);
        return updated;
    }

    private String text(SoccerAdminCommandDTO command, String fallback) {
        return command.description() == null || command.description().isBlank() ? fallback : command.description().trim();
    }

    private String substitutionText(SoccerAdminCommandDTO command) {
        if (command.description() != null && !command.description().isBlank()) {
            return command.description().trim();
        }
        return "선수 교체: in=" + command.inPlayerId() + ", out=" + command.outPlayerId();
    }

    private int minute(SoccerAdminCommandDTO command, int fallback) {
        int normalized = command.normalizedMinute();
        return normalized == 0 ? fallback : normalized;
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

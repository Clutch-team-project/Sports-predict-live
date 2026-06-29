package com.example.edu.sports_predict_live.livematch.admin.demo.service;

import com.example.edu.sports_predict_live.livematch.admin.demo.dto.DemoAutoStatusDTO;
import com.example.edu.sports_predict_live.livematch.admin.demo.entity.DemoMatchEventSeed;
import com.example.edu.sports_predict_live.livematch.admin.demo.repository.DemoMatchEventSeedRepository;
import com.example.edu.sports_predict_live.livematch.baseball.dto.BaseballLiveDTO;
import com.example.edu.sports_predict_live.livematch.baseball.service.BaseballLiveStateService;
import com.example.edu.sports_predict_live.livematch.event.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.livematch.soccer.dto.SoccerLiveDTO;
import com.example.edu.sports_predict_live.livematch.soccer.service.SoccerLiveStateService;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DemoAutoEventService {

    private final DemoMatchEventSeedRepository seedRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchRepository matchRepository;
    private final BaseballLiveStateService baseballLiveStateService;
    private final SoccerLiveStateService soccerLiveStateService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<Long, Instant> activeRuns = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public DemoAutoStatusDTO status(Long matchId) {
        requireMatch(matchId);
        return buildStatus(matchId);
    }

    @Transactional
    public DemoAutoStatusDTO start(Long matchId) {
        Match match = requireMatch(matchId);
        if (seedRepository.countByMatchIdAndPublishedFalse(matchId) == 0) {
            activeRuns.remove(matchId);
            return buildStatus(matchId);
        }
        if (!"finished".equals(match.getStatus())) {
            match.updateStatus("live");
        }
        activeRuns.put(matchId, Instant.now());
        return buildStatus(matchId);
    }

    @Transactional(readOnly = true)
    public DemoAutoStatusDTO stop(Long matchId) {
        requireMatch(matchId);
        activeRuns.remove(matchId);
        return buildStatus(matchId);
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishDueEvents() {
        Instant now = Instant.now();
        activeRuns.forEach((matchId, startedAt) -> {
            long elapsed = Math.max(0, Duration.between(startedAt, now).getSeconds());
            seedRepository.findFirstByMatchIdAndPublishedFalseAndDelaySecondsLessThanEqualOrderBySeqNoAsc(matchId, Math.toIntExact(Math.min(elapsed, Integer.MAX_VALUE)))
                    .ifPresentOrElse(seed -> publishSeed(matchId, seed), () -> stopIfDone(matchId, elapsed));
        });
    }

    private void publishSeed(Long matchId, DemoMatchEventSeed seed) {
        Match match = requireMatch(matchId);
        matchEventRepository.save(seed.toMatchEvent());
        seed.markPublished(LocalDateTime.now());

        if ("game_end".equals(seed.getEventType()) || "match_end".equals(seed.getEventType())) {
            match.updateStatus("finished");
        } else if (!"finished".equals(match.getStatus())) {
            match.updateStatus("live");
        }

        syncMatchState(match);
        if (seedRepository.countByMatchIdAndPublishedFalse(matchId) == 0) {
            activeRuns.remove(matchId);
        }
    }

    private void syncMatchState(Match match) {
        Long sportId = match.getSport() == null ? null : match.getSport().getSportId();
        if (Long.valueOf(1L).equals(sportId)) {
            BaseballLiveDTO live = baseballLiveStateService.getBaseballLive(match.getMatchId());
            if (live.scoreboard() != null) {
                match.updateScore(live.scoreboard().homeScore(), live.scoreboard().awayScore());
            }
            return;
        }
        if (Long.valueOf(2L).equals(sportId)) {
            SoccerLiveDTO live = soccerLiveStateService.getSoccerLive(match.getMatchId());
            if (live.scoreboard() != null) {
                match.updateScore(live.scoreboard().homeScore(), live.scoreboard().awayScore());
            }
            messagingTemplate.convertAndSend("/topic/games/" + match.getMatchId() + "/soccer-live", live);
        }
    }

    private void stopIfDone(Long matchId, long elapsedSeconds) {
        Integer maxDelay = seedRepository.findMaxDelaySecondsByMatchId(matchId);
        if (maxDelay == null || seedRepository.countByMatchIdAndPublishedFalse(matchId) == 0 || elapsedSeconds > maxDelay + 5L) {
            activeRuns.remove(matchId);
        }
    }

    private Match requireMatch(Long matchId) {
        return matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
    }

    private DemoAutoStatusDTO buildStatus(Long matchId) {
        long total = seedRepository.countByMatchId(matchId);
        long published = seedRepository.countByMatchIdAndPublishedTrue(matchId);
        long remaining = seedRepository.countByMatchIdAndPublishedFalse(matchId);
        long elapsed = activeRuns.containsKey(matchId)
                ? Math.max(0, Duration.between(activeRuns.get(matchId), Instant.now()).getSeconds())
                : 0;
        Integer nextDelay = seedRepository.findFirstByMatchIdAndPublishedFalseOrderBySeqNoAsc(matchId)
                .map(DemoMatchEventSeed::getDelaySeconds)
                .orElse(null);
        return new DemoAutoStatusDTO(matchId, activeRuns.containsKey(matchId), total, published, remaining, elapsed, nextDelay);
    }
}

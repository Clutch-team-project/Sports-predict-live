package com.example.edu.sports_predict_live.livematch.admin.service;

import com.example.edu.sports_predict_live.livematch.baseball.service.BaseballLiveStateService;
import com.example.edu.sports_predict_live.livematch.admin.dto.BaseballAdminAction;
import com.example.edu.sports_predict_live.livematch.admin.dto.BaseballAdminCommandDTO;
import com.example.edu.sports_predict_live.livematch.baseball.dto.BaseballLiveDTO;
import com.example.edu.sports_predict_live.livematch.event.entity.MatchEvent;
import com.example.edu.sports_predict_live.livematch.event.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.livematch.lineup.service.BaseballAdminLineupService;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BaseballAdminCommandService {

    private static final Pattern INNING_PATTERN = Pattern.compile("(\\d+)");

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final BaseballLiveStateService baseballLiveStateService;
    private final BaseballAdminLineupService baseballAdminLineupService;

    @Transactional
    public BaseballLiveDTO apply(Long matchId, BaseballAdminCommandDTO command) {
        if (command == null || command.action() == null) {
            throw new IllegalArgumentException("관리자 action이 필요합니다.");
        }
        if (command.action() == BaseballAdminAction.CLEAR_EVENTS) {
            return clearEvents(matchId);
        }
        if (command.action() == BaseballAdminAction.UNDO_LAST_EVENT) {
            return undoLastEvent(matchId);
        }

        // 선발 라인업이 완성되지 않은 경기는 진행 제어 이벤트를 등록하지 않는다.
        baseballAdminLineupService.assertReady(matchId);

        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        BaseballLiveDTO state = baseballLiveStateService.getBaseballLive(matchId);
        EventWriter writer = new EventWriter(matchId);

        switch (command.action()) {
            case START_GAME -> startGame(match, writer);
            case PAUSE_GAME -> match.updateStatus("paused");
            case RESUME_GAME -> match.updateStatus("live");
            case FINISH_GAME -> finishGame(match, state, writer);
            case START_AT_BAT -> startAtBat(match, state, command, writer);
            case BALL -> pitchBall(match, state, command, writer);
            case CALLED_STRIKE -> pitchStrike(match, state, command, writer, "called_strike", "called_strikeout");
            case SWINGING_STRIKE -> pitchStrike(match, state, command, writer, "swinging_strike", "swinging_strikeout");
            case FOUL -> pitchFoul(match, state, command, writer);
            case BUNT_FOUL -> pitchBuntFoul(match, state, command, writer);
            case SINGLE -> terminalBatterEvent(match, state, command, writer, "single", 0);
            case DOUBLE -> terminalBatterEvent(match, state, command, writer, "double", 0);
            case TRIPLE -> terminalBatterEvent(match, state, command, writer, "triple", 0);
            case HOMERUN -> homerun(match, state, command, writer);
            case WALK -> forceBatterToFirst(match, state, command, writer, "walk", "\uBCFC\uB137", "\uD0C0\uC790 \uC8FC\uC790 1\uB8E8\uB85C \uCD9C\uB8E8");
            case INTENTIONAL_WALK -> forceBatterToFirst(match, state, command, writer, "intentional_walk", "\uACE0\uC7584\uAD6C", "\uD0C0\uC790 \uC8FC\uC790 1\uB8E8\uB85C \uCD9C\uB8E8");
            case HIT_BY_PITCH -> forceBatterToFirst(match, state, command, writer, "hit_by_pitch", "몸에 맞는 공", "몸에 맞는 공으로 타자 주자 1루로 출루");
            case GROUNDOUT -> terminalBatterEvent(match, state, command, writer, "groundout", 1);
            case FLYOUT -> terminalBatterEvent(match, state, command, writer, "flyout", 1);
            case LINEOUT -> terminalBatterEvent(match, state, command, writer, "lineout", 1);
            case POPOUT -> terminalBatterEvent(match, state, command, writer, "popout", 1);
            case SAC_BUNT -> terminalBatterEvent(match, state, command, writer, "sac_bunt", 1);
            case SAC_FLY -> sacrificeFly(match, state, command, writer);
            case DOUBLE_PLAY -> terminalBatterEvent(match, state, command, writer, "double_play", 1);
            case STOLEN_BASE -> runnerEvent(match, state, command, writer, "stolen_base", 0);
            case CAUGHT_STEALING -> runnerEvent(match, state, command, writer, "caught_stealing", 1);
            case WILD_PITCH -> runnerEvent(match, state, command, writer, "wild_pitch", 0);
            case PASSED_BALL -> runnerEvent(match, state, command, writer, "passed_ball", 0);
            case BALK -> runnerEvent(match, state, command, writer, "balk", 0);
            case RUNNER_ADVANCE -> runnerEvent(match, state, command, writer, "runner_advance", 0);
            case SCORE -> runnerEvent(match, state, command, writer, "score", 0);
            case FORCE_OUT -> runnerEvent(match, state, command, writer, "force_out", 1);
            case RUNNER_OUT -> runnerEvent(match, state, command, writer, "runner_out", 1);
            case PICKOFF_OUT -> runnerEvent(match, state, command, writer, "pickoff_out", 1);
            case PITCHER_CHANGE -> pitcherChange(match, state, command, writer);
            case PINCH_HITTER -> substitution(match, state, command, writer, "pinch_hitter", battingTeamId(match, currentPeriodOrFirst(state)));
            case PINCH_RUNNER -> substitution(match, state, command, writer, "pinch_runner", battingTeamId(match, currentPeriodOrFirst(state)));
            case DEFENSIVE_SUBSTITUTION -> substitution(match, state, command, writer, "defensive_substitution", fieldingTeamId(match, currentPeriodOrFirst(state)));
            case END_INNING -> endInning(match, state, writer, true);
            default -> throw new IllegalArgumentException("지원하지 않는 관리자 action입니다: " + command.action());
        }

        matchEventRepository.flush();
        BaseballLiveDTO updated = baseballLiveStateService.getBaseballLive(matchId);
        syncMatchScore(match, updated);
        return updated;
    }

    @Transactional
    public BaseballLiveDTO clearEvents(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        matchEventRepository.deleteByMatchId(matchId);
        match.updateScore(0, 0);
        match.updateStatus("scheduled");
        matchEventRepository.flush();
        return baseballLiveStateService.getBaseballLive(matchId);
    }

    @Transactional
    public BaseballLiveDTO undoLastEvent(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        matchEventRepository.findTopByMatchIdOrderByEventTimeDescMatchEventIdDesc(matchId)
                .ifPresent(matchEventRepository::delete);
        matchEventRepository.flush();

        if (matchEventRepository.countByMatchId(matchId) == 0) {
            match.updateScore(0, 0);
            match.updateStatus("scheduled");
            return baseballLiveStateService.getBaseballLive(matchId);
        }
        BaseballLiveDTO updated = baseballLiveStateService.getBaseballLive(matchId);
        syncMatchScore(match, updated);
        return updated;
    }

    private void startGame(Match match, EventWriter writer) {
        match.updateStatus("live");
        if (matchEventRepository.countByMatchId(match.getMatchId()) == 0) {
            writer.add("1\uD68C\uCD08", match.getAwayTeam().getTeamId(), null, "inning_start", null);
        }
    }

    private void finishGame(Match match, BaseballLiveDTO state, EventWriter writer) {
        match.updateStatus("finished");
        String period = currentPeriodOrFirst(state);
        writer.add(period, battingTeamId(match, period), null, "game_end", "경기 종료");
    }

    private void startAtBat(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        Long batterId = command.playerId() != null ? command.playerId() : firstOnDeckBatter(state);
        if (batterId == null) {
            throw new IllegalArgumentException("타석을 시작할 playerId가 필요합니다.");
        }
        writer.add(period, battingTeamId(match, period), batterId, "at_bat_start", command.description());
    }

    private void pitchBall(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        writer.add(period, battingTeamId(match, period), batterId, "ball", command.normalizedPitchDescription());
        if (state.count() != null && state.count().balls() >= 3) {
            addForcedRunnerAdvances(match, state, writer, period);
            writer.add(period, battingTeamId(match, period), batterId, "walk", "\uD0C0\uC790 \uC8FC\uC790 1\uB8E8\uB85C \uCD9C\uB8E8");
        }
    }

    private void pitchStrike(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command,
                             EventWriter writer, String pitchType, String strikeoutType) {
        String period = currentPeriodOrFirst(state);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        int strikes = state.count() == null ? 0 : state.count().strikes();
        if (strikes < 2) {
            writer.add(period, battingTeamId(match, period), batterId, pitchType, command.normalizedPitchDescription());
            return;
        }
        writer.add(period, battingTeamId(match, period), batterId, strikeoutType, command.normalizedPitchDescription());
        autoEndInningIfNeeded(match, state, writer, 1);
    }

    private void pitchFoul(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        writer.add(period, battingTeamId(match, period), batterId, "foul", command.normalizedPitchDescription());
    }

    private void pitchBuntFoul(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        int strikes = state.count() == null ? 0 : state.count().strikes();
        if (strikes < 2) {
            writer.add(period, battingTeamId(match, period), batterId, "bunt_foul", command.normalizedPitchDescription());
            return;
        }
        writer.add(period, battingTeamId(match, period), batterId, "bunt_foul_strikeout", command.normalizedPitchDescription());
        autoEndInningIfNeeded(match, state, writer, 1);
    }

    private void terminalBatterEvent(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command,
                                     EventWriter writer, String eventType, int outDelta) {
        String period = currentPeriodOrFirst(state);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        // 타자주자가 도착할 베이스가 차 있어도 타격 이벤트를 먼저 저장한다.
        // 기존 주자는 이후 RUNNER_ADVANCE/SCORE/OUT 이벤트로 처리하고,
        // BaseballLiveStateService가 타자주자를 pending 상태로 보관했다가 베이스가 비면 배치한다.
        writer.add(period, battingTeamId(match, period), batterId, eventType, command.description());
        if (outDelta > 0) {
            autoEndInningIfNeeded(match, state, writer, outDelta);
        }
    }

    private void ensureBatterTargetBaseAvailable(BaseballLiveDTO state, String eventType) {
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases == null) {
            return;
        }

        String occupiedBase = null;
        if ("single".equals(eventType) && bases.first() != null) {
            occupiedBase = "1루";
        } else if ("double".equals(eventType) && bases.second() != null) {
            occupiedBase = "2루";
        } else if ("triple".equals(eventType) && bases.third() != null) {
            occupiedBase = "3루";
        }

        if (occupiedBase != null) {
            throw new IllegalStateException("타자 주자가 도착할 " + occupiedBase + "에 기존 주자가 있습니다. 기존 주자 이동/득점/아웃을 먼저 처리한 뒤 타격 결과를 저장하세요.");
        }
    }

    private void forceBatterToFirst(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command,
                                    EventWriter writer, String eventType, String defaultLabel, String defaultDescription) {
        String period = currentPeriodOrFirst(state);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        String description = command.description() == null || command.description().isBlank() ? defaultDescription : command.description();
        // 사구/볼넷 자체 이벤트를 먼저 저장하고, 그 아래에 강제 진루 이벤트를 붙인다.
        writer.add(period, battingTeamId(match, period), batterId, eventType, description == null ? defaultLabel : description);
        addForcedRunnerAdvances(match, state, writer, period);
    }

    private void addForcedRunnerAdvances(Match match, BaseballLiveDTO state, EventWriter writer, String period) {
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases == null || bases.first() == null) {
            return;
        }
        Long teamId = battingTeamId(match, period);
        if (bases.second() != null && bases.third() != null) {
            writer.add(period, teamId, bases.third().playerId(), "score", "3\uB8E8 \uC8FC\uC790 \uB4DD\uC810");
        }
        if (bases.second() != null) {
            writer.add(period, teamId, bases.second().playerId(), "runner_advance_3b", "2루 주자 3루까지 진루");
        }
        writer.add(period, teamId, bases.first().playerId(), "runner_advance_2b", "1루 주자 2루까지 진루");
    }

    private void sacrificeFly(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases == null || bases.third() == null) {
            throw new IllegalArgumentException("sac_fly requires runner on third base");
        }
        terminalBatterEvent(match, state, command, writer, "sac_fly", 1);
    }

    private void homerun(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        Long teamId = battingTeamId(match, period);
        Long batterId = requireCurrentBatter(state, command);
        ensureAtBatStarted(match, state, writer, period, batterId);
        writer.add(period, teamId, batterId, "homerun", command.description());
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases != null) {
            if (bases.third() != null) writer.add(period, teamId, bases.third().playerId(), "score", "3루 주자 득점");
            if (bases.second() != null) writer.add(period, teamId, bases.second().playerId(), "score", "2루 주자 득점");
            if (bases.first() != null) writer.add(period, teamId, bases.first().playerId(), "score", "1루 주자 득점");
        }
        writer.add(period, teamId, batterId, "score", "타자 주자 득점");
    }

    private void runnerEvent(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command,
                             EventWriter writer, String eventType, int outDelta) {
        String period = currentPeriodOrFirst(state);
        Long runnerId = command.runnerId() != null ? command.runnerId() : command.playerId();
        if (runnerId == null) {
            throw new IllegalArgumentException(eventType + " 이벤트에는 runnerId 또는 playerId가 필요합니다.");
        }
        if (!runnerExists(state, runnerId)) {
            throw new IllegalArgumentException("현재 루상에 없는 주자입니다: " + runnerId);
        }
        String normalizedEventType = normalizeRunnerAdvanceEventTypeSafe(eventType, command.base());
        String description = command.description();
        if (description == null || description.isBlank()) {
            description = defaultRunnerDescriptionSafe(state, runnerId, normalizedEventType);
        }
        writer.add(period, battingTeamId(match, period), runnerId, normalizedEventType, description);
        if (outDelta > 0) {
            autoEndInningIfNeeded(match, state, writer, outDelta);
        }
    }


    private String normalizeRunnerAdvanceEventType(String eventType, String targetBase) {
        if (!"runner_advance".equals(eventType)) {
            return eventType;
        }
        String base = targetBase == null ? "" : targetBase.trim().toLowerCase();
        return switch (base) {
            case "second", "2", "2b", "2루" -> "runner_advance_2b";
            case "third", "3", "3b", "3루" -> "runner_advance_3b";
            case "home", "4", "score", "홈" -> "score";
            default -> "runner_advance";
        };
    }

    private String defaultRunnerDescription(BaseballLiveDTO state, Long runnerId, String eventType, String targetBase) {
        String from = currentRunnerBaseText(state, runnerId);
        if ("runner_advance_2b".equals(eventType)) {
            return from + " 주자 2루까지 진루";
        }
        if ("runner_advance_3b".equals(eventType)) {
            return from + " 주자 3루까지 진루";
        }
        if ("score".equals(eventType)) {
            return from + " 주자 득점";
        }
        return from + " 주자 진루";
    }

    private String currentRunnerBaseText(BaseballLiveDTO state, Long runnerId) {
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases == null || runnerId == null) {
            return "주자";
        }
        if (bases.first() != null && Objects.equals(bases.first().playerId(), runnerId)) {
            return "1루";
        }
        if (bases.second() != null && Objects.equals(bases.second().playerId(), runnerId)) {
            return "2루";
        }
        if (bases.third() != null && Objects.equals(bases.third().playerId(), runnerId)) {
            return "3루";
        }
        return "주자";
    }

    private void pitcherChange(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        if (command.playerId() == null) {
            throw new IllegalArgumentException("투수 교체에는 playerId가 필요합니다.");
        }
        writer.add(period, fieldingTeamId(match, period), command.playerId(), "pitcher_change", command.description());
    }

    private String normalizeRunnerAdvanceEventTypeSafe(String eventType, String targetBase) {
        if (!"runner_advance".equals(eventType)) {
            return eventType;
        }
        String base = targetBase == null ? "" : targetBase.trim().toLowerCase();
        return switch (base) {
            case "second", "2", "2b", "2루" -> "runner_advance_2b";
            case "third", "3", "3b", "3루" -> "runner_advance_3b";
            case "home", "4", "score", "홈" -> "score";
            default -> "runner_advance";
        };
    }

    private String defaultRunnerDescriptionSafe(BaseballLiveDTO state, Long runnerId, String eventType) {
        String from = currentRunnerBaseTextSafe(state, runnerId);
        if ("runner_advance_2b".equals(eventType)) {
            return from + " 주자 2루까지 진루";
        }
        if ("runner_advance_3b".equals(eventType)) {
            return from + " 주자 3루까지 진루";
        }
        if ("score".equals(eventType)) {
            return from + " 주자 득점";
        }
        return from + " 주자 진루";
    }

    private String currentRunnerBaseTextSafe(BaseballLiveDTO state, Long runnerId) {
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases == null || runnerId == null) {
            return "주자";
        }
        if (bases.first() != null && Objects.equals(bases.first().playerId(), runnerId)) {
            return "1루";
        }
        if (bases.second() != null && Objects.equals(bases.second().playerId(), runnerId)) {
            return "2루";
        }
        if (bases.third() != null && Objects.equals(bases.third().playerId(), runnerId)) {
            return "3루";
        }
        return "주자";
    }

    private void substitution(Match match, BaseballLiveDTO state, BaseballAdminCommandDTO command,
                              EventWriter writer, String eventType, Long teamId) {
        String period = currentPeriodOrFirst(state);
        if (command.playerId() == null) {
            throw new IllegalArgumentException(eventType + " 이벤트에는 playerId가 필요합니다.");
        }
        writer.add(period, teamId, command.playerId(), eventType, command.description());
    }

    private void endInning(Match match, BaseballLiveDTO state, EventWriter writer, boolean strict) {
        int outs = state.count() == null ? 0 : state.count().outs();
        if (strict && outs < 3) {
            throw new IllegalArgumentException("3아웃 전에 이닝 종료를 저장할 수 없습니다. 현재 아웃: " + outs);
        }
        String period = currentPeriodOrFirst(state);
        writer.add(period, battingTeamId(match, period), null, "inning_end", "이닝 종료");
        String next = nextPeriod(period);
        writer.add(next, battingTeamId(match, next), null, "inning_start", "이닝 시작");
    }

    private void ensureAtBatStarted(Match match, BaseballLiveDTO state, EventWriter writer, String period, Long batterId) {
        if (batterId == null || hasCurrentAtBat(state, batterId)) {
            return;
        }
        writer.add(period, battingTeamId(match, period), batterId, "at_bat_start", null);
    }

    private boolean hasCurrentAtBat(BaseballLiveDTO state, Long batterId) {
        if (state.timeline() == null) {
            return false;
        }
        return state.timeline().stream()
                .anyMatch(card -> card.current() && Objects.equals(card.batterId(), batterId));
    }

    private boolean autoEndInningIfNeeded(Match match, BaseballLiveDTO state, EventWriter writer, int outDelta) {
        int outs = state.count() == null ? 0 : state.count().outs();
        if (outs + outDelta >= 3) {
            endInning(match, state, writer, false);
            return true;
        }
        return false;
    }

    private void startNextAtBatIfPossible(Match match, BaseballLiveDTO state, EventWriter writer) {
        String period = currentPeriodOrFirst(state);
        Long currentBatterId = state.timeline() == null ? null : state.timeline().stream()
                .filter(BaseballLiveDTO.AtBatCard::current)
                .map(BaseballLiveDTO.AtBatCard::batterId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        Long nextBatterId = nextOnDeckBatter(state, currentBatterId);
        if (nextBatterId != null) {
            writer.add(period, battingTeamId(match, period), nextBatterId, "at_bat_start", null);
        }
    }

    private Long requireCurrentBatter(BaseballLiveDTO state, BaseballAdminCommandDTO command) {
        if (command.playerId() != null) return command.playerId();
        if (state.timeline() != null) {
            return state.timeline().stream()
                    .filter(BaseballLiveDTO.AtBatCard::current)
                    .map(BaseballLiveDTO.AtBatCard::batterId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("현재 타석이 없습니다. 먼저 START_AT_BAT을 실행하세요."));
        }
        throw new IllegalArgumentException("현재 타석이 없습니다. 먼저 START_AT_BAT을 실행하세요.");
    }

    private Long firstOnDeckBatter(BaseballLiveDTO state) {
        if (state.onDeck() == null || state.onDeck().isEmpty()) return null;
        return state.onDeck().get(0).playerId();
    }

    private Long nextOnDeckBatter(BaseballLiveDTO state, Long currentBatterId) {
        if (state.onDeck() == null || state.onDeck().isEmpty()) return null;
        return state.onDeck().stream()
                .map(BaseballLiveDTO.LineupPlayer::playerId)
                .filter(Objects::nonNull)
                .filter(playerId -> !Objects.equals(playerId, currentBatterId))
                .findFirst()
                .orElse(null);
    }

    private boolean runnerExists(BaseballLiveDTO state, Long runnerId) {
        BaseballLiveDTO.BaseState bases = state.baseState();
        if (bases == null || runnerId == null) return false;
        return (bases.first() != null && Objects.equals(bases.first().playerId(), runnerId))
                || (bases.second() != null && Objects.equals(bases.second().playerId(), runnerId))
                || (bases.third() != null && Objects.equals(bases.third().playerId(), runnerId));
    }

    private String currentPeriodOrFirst(BaseballLiveDTO state) {
        String period = state.currentPeriod();
        if (period == null || period.isBlank() || !period.matches("\\d+\uD68C[\uCD08\uB9D0]")) {
            return "1\uD68C\uCD08";
        }
        return period;
    }

    private Long battingTeamId(Match match, String period) {
        return isBottom(period) ? match.getHomeTeam().getTeamId() : match.getAwayTeam().getTeamId();
    }

    private Long fieldingTeamId(Match match, String period) {
        return isBottom(period) ? match.getAwayTeam().getTeamId() : match.getHomeTeam().getTeamId();
    }

    private boolean isBottom(String period) {
        return period != null && period.contains("\uB9D0");
    }

    private String nextPeriod(String period) {
        int inning = inningNumber(period);
        if (inning <= 0) return "1\uD68C\uCD08";
        return isBottom(period) ? (inning + 1) + "\uD68C\uCD08" : inning + "\uD68C\uB9D0";
    }

    private int inningNumber(String period) {
        if (period == null) return 1;
        Matcher matcher = INNING_PATTERN.matcher(period);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
    }

    private int periodBaseTime(String period) {
        int inning = inningNumber(period);
        int half = isBottom(period) ? 1 : 0;
        return inning * 10000 + half * 1000;
    }

    private void syncMatchScore(Match match, BaseballLiveDTO updated) {
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

        private void add(String period, Long teamId, Long playerId, String eventType, String description) {
            int nextTime = nextTimes.compute(period, (key, current) -> {
                if (current != null) return current + 1;
                return matchEventRepository
                        .findTopByMatchIdAndEventPeriodOrderByEventTimeDescMatchEventIdDesc(matchId, period)
                        .map(MatchEvent::getEventTime)
                        .orElse(periodBaseTime(period)) + 1;
            });
            matchEventRepository.save(MatchEvent.create(matchId, teamId, playerId, eventType, period, nextTime, description));
        }
    }
}



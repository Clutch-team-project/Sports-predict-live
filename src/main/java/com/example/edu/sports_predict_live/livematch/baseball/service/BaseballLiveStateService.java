package com.example.edu.sports_predict_live.livematch.baseball.service;

import com.example.edu.sports_predict_live.livematch.baseball.dto.BaseballLiveDTO;
import com.example.edu.sports_predict_live.livematch.lineup.dto.MatchLineupDTO;
import com.example.edu.sports_predict_live.livematch.event.entity.MatchEvent;
import com.example.edu.sports_predict_live.livematch.lineup.entity.MatchLineup;
import com.example.edu.sports_predict_live.livematch.event.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.livematch.lineup.repository.MatchLineupRepository;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import com.example.edu.sports_predict_live.prediction.service.PredictionService;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseballLiveStateService {

    private static final Pattern INNING_PATTERN = Pattern.compile("(\\d+)");
    private static final Set<String> HIT_TYPES = Set.of("single", "double", "triple", "homerun");
    private static final Set<String> WALK_TYPES = Set.of("walk", "intentional_walk");
    private static final Set<String> ERROR_TYPES = Set.of("field_error", "defensive_error", "error");
    private static final Set<String> RBI_ALLOWED_TYPES = Set.of(
            "single", "double", "triple", "homerun", "sac_fly", "sac_bunt",
            "groundout", "flyout", "lineout", "fielder_choice", "walk",
            "intentional_walk", "hit_by_pitch"
    );
    private static final Set<String> RBI_EXCLUDED_TYPES = Set.of(
            "field_error", "defensive_error", "error", "wild_pitch", "passed_ball",
            "balk", "stolen_base", "caught_stealing", "double_play", "triple_play"
    );
    private static final Set<String> STRIKEOUT_TYPES = Set.of(
            "called_strikeout", "swinging_strikeout", "foul_tip_strikeout",
            "bunt_foul_strikeout", "pitch_clock_strikeout",
            "dropped_third_strike_safe", "dropped_third_strike_out"
    );
    private static final Set<String> TERMINAL_TYPES = new HashSet<>(Set.of(
            "single", "double", "triple", "homerun", "walk", "intentional_walk", "hit_by_pitch",
            "fielder_choice", "field_error", "groundout", "flyout", "lineout", "popout",
            "double_play", "triple_play", "sac_bunt", "sac_fly",
            "dropped_third_strike_safe", "dropped_third_strike_out"
    ));
    private static final Set<String> ACTUAL_PITCH_TYPES = Set.of(
            "ball", "called_strike", "swinging_strike", "check_swing_strike", "foul", "foul_tip",
            "bunt_foul", "single", "double", "triple", "homerun", "field_error", "fielder_choice",
            "groundout", "flyout", "lineout", "popout", "double_play", "triple_play", "sac_bunt",
            "sac_fly", "called_strikeout", "swinging_strikeout", "foul_tip_strikeout",
            "bunt_foul_strikeout", "dropped_third_strike_safe", "dropped_third_strike_out",
            "hit_by_pitch"
    );
    private static final Set<String> OUT_TYPES = new HashSet<>(Set.of(
            "groundout", "flyout", "lineout", "popout", "sac_bunt", "sac_fly", "double_play",
            "triple_play", "caught_stealing", "pickoff_out", "force_out", "runner_out"
    ));
    private static final Set<String> DEFENSE_POSITIONS = Set.of("P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF");

    static {
        TERMINAL_TYPES.addAll(STRIKEOUT_TYPES);
        OUT_TYPES.addAll(STRIKEOUT_TYPES);
    }

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final PlayerRepository playerRepository;
    private final PlayerSeasonStatBaseballRepository playerSeasonStatBaseballRepository;
    private final TeamRepository teamRepository;
    private final PredictionService predictionService;

    public BaseballLiveDTO getBaseballLive(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        List<MatchEvent> events = matchEventRepository.findByMatchIdOrderByEventTimeAscMatchEventIdAsc(matchId);
        List<MatchLineup> lineups = matchLineupRepository.findByMatchIdOrderByTeamIdAscStarterDescOrderNumAscMatchLineupIdAsc(matchId);

        Map<Long, Player> playerById = loadPlayerMap(events, lineups);
        Map<Long, PlayerSeasonStatBaseball> seasonStatByPlayerId = loadSeasonStatMap(playerById.keySet(), match.getSeason());
        Map<Long, Team> teamById = loadTeamMap(match, lineups);
        List<MatchLineupDTO> lineupDtos = lineups.stream()
                .map(lineup -> MatchLineupDTO.from(lineup, teamById.get(lineup.getTeamId()), playerById.get(lineup.getPlayerId())))
                .toList();

        State state = buildState(match, events, lineups, playerById, teamById, seasonStatByPlayerId);
        Long fieldingTeamId = fieldingTeamId(match, state.currentPeriod);
        Long battingTeamId = battingTeamId(match, state.currentPeriod);

        return new BaseballLiveDTO(
                match.getMatchId(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getVenue(),
                match.getWinningPitcher(),
                match.getLosingPitcher(),
                state.currentPeriod,
                teamInfo(match.getHomeTeam()),
                teamInfo(match.getAwayTeam()),
                state.scoreboard(match),
                new BaseballLiveDTO.Count(state.balls, Math.min(state.strikes, 2), state.outs),
                state.baseState(),
                state.currentPitcherStat,
                currentBatter(state, playerById),
                fielders(lineups, playerById, fieldingTeamId),
                onDeck(lineups, playerById, battingTeamId, state.lastBatterId),
                atBatCards(state.atBats, state.playerStats, state.currentAtBat),
                events.size(),
                state.playerStats.values().stream().map(PlayerStatBuilder::toDto).toList(),
                state.pitcherStats.values().stream().map(PitcherStatBuilder::toDto).toList(),
                lineupDtos,
                predictionService.getMatchSummary(matchId)
        );
    }

    private State buildState(Match match, List<MatchEvent> events, List<MatchLineup> lineups,
                             Map<Long, Player> playerById, Map<Long, Team> teamById,
                             Map<Long, PlayerSeasonStatBaseball> seasonStatByPlayerId) {
        State state = new State(match, playerById, teamById, seasonStatByPlayerId);
        String lastPeriod = events.isEmpty() ? "PRE_GAME" : null;

        for (MatchEvent event : events) {
            String type = type(event);
            String period = blankToDefault(event.getEventPeriod(), lastPeriod == null ? "PRE_GAME" : lastPeriod);
            state.currentPeriod = period;
            lastPeriod = period;

            Long defenseTeamId = fieldingTeamId(match, period);
            Long battingTeamId = battingTeamId(match, period);
            state.ensurePitcher(defenseTeamId, starterPitcher(lineups, playerById, teamById, defenseTeamId, seasonStatByPlayerId));

            if ("inning_start".equals(type)) {
                state.resetCount();
            }
            if ("at_bat_start".equals(type)) {
                state.finalizeCurrentAtBatRbi();
                state.resetAtBat(event, playerById, lineups, battingTeamId);
            }
            if ("pitcher_change".equals(type)) {
                state.changePitcher(defenseTeamId, pitcherFromPlayer(event.getPlayerId(), defenseTeamId, playerById, teamById, seasonStatByPlayerId));
            }

            PitcherStatBuilder pitcher = state.currentPitcher(defenseTeamId);
            if (ACTUAL_PITCH_TYPES.contains(type) && pitcher != null) {
                pitcher.pitchCount++;
            }
            if (HIT_TYPES.contains(type) && pitcher != null) {
                pitcher.hitsAllowed++;
            }
            if (WALK_TYPES.contains(type) && pitcher != null) {
                pitcher.walksAllowed++;
            }
            if ("hit_by_pitch".equals(type) && pitcher != null) {
                pitcher.hitByPitchAllowed++;
            }
            if ("homerun".equals(type) && pitcher != null) {
                pitcher.homeRunsAllowed++;
            }
            if ("score".equals(type) && pitcher != null) {
                pitcher.runsAllowed++;
            }
            if (STRIKEOUT_TYPES.contains(type) && pitcher != null) {
                pitcher.strikeouts++;
            }

            state.applyCount(type);
            int outDelta = outDelta(type);
            if (outDelta > 0 && pitcher != null) {
                pitcher.outsPitched += outDelta;
            }
            state.outs = Math.min(3, state.outs + outDelta);

            state.applyScoreAndTeamTotals(event, battingTeamId);
            state.applyBases(event, type, playerById);
            state.applyPlayerStats(event, lineups, type, playerById, battingTeamId);
            state.addPitchRow(event, type);

            if ("inning_end".equals(type)) {
                state.finalizeCurrentAtBatRbi();
                state.resetCount();
                state.clearBases();
            }
        }

        state.finalizeCurrentAtBatRbi();
        state.currentPitcherStat = Optional.ofNullable(state.currentPitcher(fieldingTeamId(match, state.currentPeriod)))
                .map(PitcherStatBuilder::toDto)
                .orElse(null);
        return state;
    }

    private BaseballLiveDTO.BatterInfo currentBatter(
            State state,
            Map<Long, Player> playerById
    ) {
        if (state.currentAtBat == null || state.currentAtBat.batterId == null) {
            return null;
        }

        Player player = playerById.get(state.currentAtBat.batterId);
        if (player == null) {
            return null;
        }

        Team team = player.getTeam();

        return new BaseballLiveDTO.BatterInfo(
                player.getPlayerId(),
                player.getName(),
                team != null ? team.getTeamId() : null,
                team != null ? team.getName() : null,
                state.currentAtBat.orderNum,
                state.currentAtBat.position
        );
    }

    private Map<Long, Player> loadPlayerMap(List<MatchEvent> events, List<MatchLineup> lineups) {
        Set<Long> ids = new HashSet<>();
        events.stream().map(MatchEvent::getPlayerId).filter(Objects::nonNull).forEach(ids::add);
        lineups.stream().map(MatchLineup::getPlayerId).filter(Objects::nonNull).forEach(ids::add);
        return playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getPlayerId, player -> player));
    }

    private Map<Long, PlayerSeasonStatBaseball> loadSeasonStatMap(Set<Long> playerIds, String season) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Map.of();
        }
        String targetSeason = season == null || season.isBlank() ? "2026" : season;
        return playerSeasonStatBaseballRepository.findByPlayerIdsAndSeason(playerIds, targetSeason).stream()
                .filter(stat -> stat.getPlayerSeasonStat() != null)
                .filter(stat -> stat.getPlayerSeasonStat().getPlayer() != null)
                .collect(Collectors.toMap(
                        stat -> stat.getPlayerSeasonStat().getPlayer().getPlayerId(),
                        stat -> stat,
                        (left, right) -> left
                ));
    }

    private Map<Long, Team> loadTeamMap(Match match, List<MatchLineup> lineups) {
        Set<Long> ids = new HashSet<>();
        ids.add(match.getHomeTeam().getTeamId());
        ids.add(match.getAwayTeam().getTeamId());
        lineups.stream().map(MatchLineup::getTeamId).filter(Objects::nonNull).forEach(ids::add);
        return teamRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Team::getTeamId, team -> team));
    }

    private BaseballLiveDTO.TeamInfo teamInfo(Team team) {
        String name = team.getName();
        return new BaseballLiveDTO.TeamInfo(
                team.getTeamId(),
                name,
                name == null || name.isBlank() ? "-" : name.substring(0, 1),
                team.getEmblemUrl()
        );
    }

    private List<BaseballLiveDTO.Fielder> fielders(List<MatchLineup> lineups, Map<Long, Player> playerById, Long teamId) {
        return lineups.stream()
                .filter(MatchLineup::isStarter)
                .filter(lineup -> Objects.equals(lineup.getTeamId(), teamId))
                .filter(lineup -> DEFENSE_POSITIONS.contains(positionCode(lineup.getPosition())))
                .sorted(Comparator.comparing(lineup -> positionOrder(positionCode(lineup.getPosition()))))
                .map(lineup -> {
                    Player player = playerById.get(lineup.getPlayerId());
                    String name = playerName(player, lineup.getPlayerId());
                    return new BaseballLiveDTO.Fielder(lineup.getPlayerId(), name, name, lineup.getTeamId(), positionCode(lineup.getPosition()));
                })
                .toList();
    }

    private List<BaseballLiveDTO.LineupPlayer> onDeck(List<MatchLineup> lineups, Map<Long, Player> playerById,
                                                      Long battingTeamId, Long lastBatterId) {
        List<MatchLineup> battingOrder = lineups.stream()
                .filter(MatchLineup::isStarter)
                .filter(lineup -> Objects.equals(lineup.getTeamId(), battingTeamId))
                .filter(lineup -> lineup.getOrderNum() != null && lineup.getOrderNum() >= 1 && lineup.getOrderNum() <= 9)
                .sorted(Comparator.comparing(MatchLineup::getOrderNum))
                .toList();
        if (battingOrder.isEmpty()) {
            return List.of();
        }

        int start = 0;
        if (lastBatterId != null) {
            for (int i = 0; i < battingOrder.size(); i++) {
                if (Objects.equals(battingOrder.get(i).getPlayerId(), lastBatterId)) {
                    start = (i + 1) % battingOrder.size();
                    break;
                }
            }
        }

        List<BaseballLiveDTO.LineupPlayer> result = new ArrayList<>();
        for (int i = 0; i < Math.min(3, battingOrder.size()); i++) {
            MatchLineup lineup = battingOrder.get((start + i) % battingOrder.size());
            result.add(lineupPlayer(lineup, playerById));
        }
        return result;
    }

    private BaseballLiveDTO.LineupPlayer lineupPlayer(MatchLineup lineup, Map<Long, Player> playerById) {
        String name = playerName(playerById.get(lineup.getPlayerId()), lineup.getPlayerId());
        return new BaseballLiveDTO.LineupPlayer(
                lineup.getPlayerId(),
                name,
                name,
                lineup.getTeamId(),
                lineup.getOrderNum(),
                positionCode(lineup.getPosition()),
                lineup.isStarter()
        );
    }

    private List<BaseballLiveDTO.AtBatCard> atBatCards(List<AtBatBuilder> atBats,
                                                       Map<Long, PlayerStatBuilder> playerStats,
                                                       AtBatBuilder currentAtBat) {
        return atBats.stream()
                .sorted(Comparator.comparing((AtBatBuilder atBat) -> atBat.order).reversed())
                .map(atBat -> atBat.toDto(playerStats.get(atBat.batterId), atBat == currentAtBat && !atBat.terminalApplied))
                .toList();
    }

    private PitcherStatBuilder starterPitcher(List<MatchLineup> lineups, Map<Long, Player> playerById,
                                              Map<Long, Team> teamById, Long defenseTeamId,
                                              Map<Long, PlayerSeasonStatBaseball> seasonStatByPlayerId) {
        return lineups.stream()
                .filter(MatchLineup::isStarter)
                .filter(lineup -> Objects.equals(lineup.getTeamId(), defenseTeamId))
                .filter(lineup -> "P".equals(positionCode(lineup.getPosition())))
                .findFirst()
                .map(lineup -> pitcherFromPlayer(lineup.getPlayerId(), defenseTeamId, playerById, teamById, seasonStatByPlayerId))
                .orElse(null);
    }

    private PitcherStatBuilder pitcherFromPlayer(Long playerId, Long teamId, Map<Long, Player> playerById, Map<Long, Team> teamById,
                                                 Map<Long, PlayerSeasonStatBaseball> seasonStatByPlayerId) {
        if (playerId == null) {
            return null;
        }
        Player player = playerById.get(playerId);
        Team team = teamById.get(teamId);
        return new PitcherStatBuilder(
                playerId,
                playerName(player, playerId),
                teamId,
                team != null ? team.getName() : null,
                player != null ? positionCode(player.getPosition()) : "P",
                seasonStatByPlayerId.get(playerId)
        );
    }

    private Long fieldingTeamId(Match match, String period) {
        return isBottom(period) ? match.getAwayTeam().getTeamId() : match.getHomeTeam().getTeamId();
    }

    private Long battingTeamId(Match match, String period) {
        return isBottom(period) ? match.getHomeTeam().getTeamId() : match.getAwayTeam().getTeamId();
    }

    private boolean isBottom(String period) {
        return period != null && period.contains("말");
    }

    private String nextPeriod(String period) {
        int inning = inningNumber(period);
        if (inning <= 0) {
            return "1회초";
        }
        return isBottom(period) ? (inning + 1) + "회초" : inning + "회말";
    }

    private int inningNumber(String period) {
        if (period == null) return 0;
        Matcher matcher = INNING_PATTERN.matcher(period);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String type(MatchEvent event) {
        return event.getEventType() == null ? "" : event.getEventType().trim().toLowerCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String playerName(Player player, Long playerId) {
        if (player != null && player.getName() != null) return player.getName();
        return playerId == null ? "-" : "선수 " + playerId;
    }

    private String positionCode(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("SP") || value.equals("RP") || value.equals("RHP") || value.equals("LHP")
                || value.contains("투수") || value.contains("우완") || value.contains("좌완")) return "P";
        if (value.contains("포수")) return "C";
        if (value.contains("1루")) return "1B";
        if (value.contains("2루")) return "2B";
        if (value.contains("3루")) return "3B";
        if (value.contains("유격")) return "SS";
        if (value.contains("좌익")) return "LF";
        if (value.contains("중견")) return "CF";
        if (value.contains("우익")) return "RF";
        return value.isBlank() ? "-" : value;
    }

    private int positionOrder(String position) {
        return switch (position) {
            case "P" -> 1;
            case "C" -> 2;
            case "1B" -> 3;
            case "2B" -> 4;
            case "3B" -> 5;
            case "SS" -> 6;
            case "LF" -> 7;
            case "CF" -> 8;
            case "RF" -> 9;
            default -> 99;
        };
    }

    // 더블 플레이 트리플 플레이에 대한 아웃처리 로직 수정 전부다 아웃카운트 하나로 처리.
    private int outDelta(String type) {
        return OUT_TYPES.contains(type) ? 1 : 0;
    }

    private boolean isBatterEvent(String type) {
        return ACTUAL_PITCH_TYPES.contains(type)
                || TERMINAL_TYPES.contains(type)
                || "walk".equals(type)
                || "intentional_walk".equals(type)
                || "hit_by_pitch".equals(type);
    }

    private String eventLabel(String type) {
        return switch (type) {
            case "ball", "pitch_clock_ball" -> "\uBCFC";
            case "called_strike", "check_swing_strike" -> "\uC2A4\uD2B8\uB77C\uC774\uD06C";
            case "swinging_strike" -> "\uD5DB\uC2A4\uC719";
            case "pitch_clock_strike" -> "\uD53C\uCE58\uD074\uB77D \uC2A4\uD2B8\uB77C\uC774\uD06C";
            case "foul" -> "\uD30C\uC6B8";
            case "foul_tip" -> "\uD30C\uC6B8\uD301";
            case "bunt_foul" -> "\uBC88\uD2B8 \uD30C\uC6B8";
            case "single", "double", "triple", "homerun", "field_error", "fielder_choice" -> "\uD0C0\uACA9";
            case "walk" -> "\uBCFC\uB137";
            case "intentional_walk" -> "\uACE0\uC7584\uAD6C";
            case "hit_by_pitch" -> "\uC0AC\uAD6C";
            case "groundout", "flyout", "lineout", "popout", "double_play", "triple_play", "sac_bunt", "sac_fly" -> "\uC544\uC6C3";
            case "stolen_base" -> "도루 성공";
            case "caught_stealing" -> "도루 실패";
            case "runner_advance", "runner_advance_2b", "runner_advance_3b" -> "주자 진루";
            case "score" -> "득점";
            case "runner_out" -> "주자 아웃";
            case "force_out" -> "포스 아웃";
            case "pickoff_out" -> "견제 아웃";
            case "wild_pitch" -> "폭투";
            case "passed_ball" -> "포일";
            case "balk" -> "보크";
            case "pitcher_change" -> "\uD22C\uC218 \uAD50\uCCB4";
            case "inning_end" -> "\uC774\uB2DD \uC885\uB8CC";
            default -> "swinging_strikeout".equals(type) ? "\uD5DB\uC2A4\uC719 \uC0BC\uC9C4" : STRIKEOUT_TYPES.contains(type) ? "\uC0BC\uC9C4" : type;
        };
    }
    private String countText(int balls, int strikes) {
        return balls + " - " + Math.min(strikes, 2);
    }

    private String inningsPitched(int outs) {
        return (outs / 3) + "." + (outs % 3) + "\uC774\uB2DD";
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private class State {
        private final Match match;
        private final Map<Long, Player> playerById;
        private final Map<Long, Team> teamById;
        private final Map<Long, PlayerSeasonStatBaseball> seasonStatByPlayerId;
        private final Map<Long, int[]> inningRunsByTeam = new HashMap<>();
        private final Map<Long, TeamTotals> teamTotals = new HashMap<>();
        private final Map<Long, PlayerStatBuilder> playerStats = new LinkedHashMap<>();
        private final Map<Long, PitcherStatBuilder> pitcherStats = new LinkedHashMap<>();
        private final Map<Long, PitcherStatBuilder> currentPitcherByTeam = new HashMap<>();
        private final Map<Long, Integer> battingCursorByTeam = new HashMap<>();
        private final List<AtBatBuilder> atBats = new ArrayList<>();
        private String currentPeriod = "경기 전";
        private int balls = 0;
        private int strikes = 0;
        private int outs = 0;
        private BaseballLiveDTO.BaseRunner first;
        private BaseballLiveDTO.BaseRunner second;
        private BaseballLiveDTO.BaseRunner third;
        private BaseballLiveDTO.BaseRunner pendingBatterRunner;
        private int pendingBatterTargetBase;
        private AtBatBuilder currentAtBat;
        private Long lastBatterId;
        private BaseballLiveDTO.PitcherGameStat currentPitcherStat;

        private State(Match match, Map<Long, Player> playerById, Map<Long, Team> teamById,
                      Map<Long, PlayerSeasonStatBaseball> seasonStatByPlayerId) {
            this.match = match;
            this.playerById = playerById;
            this.teamById = teamById;
            this.seasonStatByPlayerId = seasonStatByPlayerId;
            teamTotals.put(match.getHomeTeam().getTeamId(), new TeamTotals());
            teamTotals.put(match.getAwayTeam().getTeamId(), new TeamTotals());
            inningRunsByTeam.put(match.getHomeTeam().getTeamId(), new int[9]);
            inningRunsByTeam.put(match.getAwayTeam().getTeamId(), new int[9]);
        }

        private void resetCount() {
            balls = 0;
            strikes = 0;
            outs = 0;
        }

        private void resetAtBat(MatchEvent event, Map<Long, Player> playerById, List<MatchLineup> lineups, Long battingTeamId) {
            balls = 0;
            strikes = 0;
            Long batterId = event.getPlayerId() != null ? event.getPlayerId() : nextBatterId(lineups, battingTeamId);
            lastBatterId = batterId;
            currentAtBat = new AtBatBuilder(atBats.size() + 1, event.getEventPeriod(), batterId, playerById, lineups);
            if (batterId != null) {
                currentAtBat.battingAvg = playerStat(batterId, battingTeamId, playerById, lineups).battingAverageText();
            }
            atBats.add(currentAtBat);

        }

        private Long nextBatterId(List<MatchLineup> lineups, Long battingTeamId) {
            List<MatchLineup> battingOrder = lineups.stream()
                    .filter(MatchLineup::isStarter)
                    .filter(lineup -> Objects.equals(lineup.getTeamId(), battingTeamId))
                    .filter(lineup -> lineup.getOrderNum() != null && lineup.getOrderNum() >= 1 && lineup.getOrderNum() <= 9)
                    .sorted(Comparator.comparing(MatchLineup::getOrderNum))
                    .toList();
            if (battingOrder.isEmpty()) return null;
            int cursor = battingCursorByTeam.getOrDefault(battingTeamId, 0);
            MatchLineup batter = battingOrder.get(cursor % battingOrder.size());
            battingCursorByTeam.put(battingTeamId, cursor + 1);
            return batter.getPlayerId();
        }

        private void ensurePitcher(Long teamId, PitcherStatBuilder starter) {
            if (teamId == null || currentPitcherByTeam.containsKey(teamId) || starter == null) return;
            currentPitcherByTeam.put(teamId, statsPitcher(starter));
        }

        private PitcherStatBuilder currentPitcher(Long defenseTeamId) {
            return currentPitcherByTeam.get(defenseTeamId);
        }

        private void changePitcher(Long teamId, PitcherStatBuilder pitcher) {
            if (teamId == null || pitcher == null) return;
            currentPitcherByTeam.put(teamId, statsPitcher(pitcher));
        }

        private PitcherStatBuilder statsPitcher(PitcherStatBuilder source) {
            return pitcherStats.computeIfAbsent(source.playerId, ignored -> source);
        }

        private void applyCount(String type) {
            switch (type) {
                case "ball", "pitch_clock_ball" -> balls++;
                case "called_strike", "swinging_strike", "check_swing_strike", "pitch_clock_strike", "foul_tip", "bunt_foul" -> strikes++;
                case "foul" -> {
                    if (strikes < 2) strikes++;
                }
                default -> {
                }
            }
            if (TERMINAL_TYPES.contains(type)) {
                balls = 0;
                strikes = 0;
            }
        }

        private void applyScoreAndTeamTotals(MatchEvent event, Long battingTeamId) {
            Long teamId = event.getTeamId() != null ? event.getTeamId() : battingTeamId;
            TeamTotals totals = teamTotals.computeIfAbsent(teamId, ignored -> new TeamTotals());
            String type = type(event);
            if ("score".equals(type)) {
                totals.runs++;
                int inningIndex = inningNumber(event.getEventPeriod()) - 1;
                if (inningIndex >= 0 && inningIndex < 9) {
                    inningRunsByTeam.computeIfAbsent(teamId, ignored -> new int[9])[inningIndex]++;
                }
            }
            if (HIT_TYPES.contains(type)) totals.hits++;
            if (WALK_TYPES.contains(type)) totals.walks++;
            if (ERROR_TYPES.contains(type)) totals.errors++;
        }

        private void applyBases(MatchEvent event, String type, Map<Long, Player> playerById) {
            if ("score".equals(type)) {
                removeRunner(event.getPlayerId());
                placePendingBatterIfPossible();
                return;
            }
            if ("stolen_base".equals(type) || "runner_advance".equals(type)) {
                advanceExistingRunner(event.getPlayerId(), 1);
                placePendingBatterIfPossible();
                return;
            }
            if ("runner_advance_2b".equals(type)) {
                moveExistingRunnerTo(event.getPlayerId(), 2);
                placePendingBatterIfPossible();
                return;
            }
            if ("runner_advance_3b".equals(type)) {
                moveExistingRunnerTo(event.getPlayerId(), 3);
                placePendingBatterIfPossible();
                return;
            }
            if ("caught_stealing".equals(type) || "pickoff_out".equals(type) || "force_out".equals(type) || "runner_out".equals(type)) {
                removeRunner(event.getPlayerId());
                placePendingBatterIfPossible();
                return;
            }
            if (event.getPlayerId() == null) return;
            BaseballLiveDTO.BaseRunner batter = new BaseballLiveDTO.BaseRunner(event.getPlayerId(), playerName(playerById.get(event.getPlayerId()), event.getPlayerId()));
            switch (type) {
                case "walk", "intentional_walk", "hit_by_pitch", "single", "field_error", "fielder_choice", "dropped_third_strike_safe" -> placeBatterOrHold(batter, 1);
                case "double" -> placeBatterOrHold(batter, 2);
                case "triple" -> placeBatterOrHold(batter, 3);
                case "homerun" -> {
                    removeRunner(event.getPlayerId());
                    clearPendingBatter();
                }
                default -> {
                }
            }
        }

        private void placeBatterOrHold(BaseballLiveDTO.BaseRunner batter, int targetBase) {
            if (batter == null) return;
            if (isBaseEmptyOrSameRunner(targetBase, batter.playerId())) {
                putRunnerOnBase(targetBase, batter);
                if (pendingBatterRunner != null && Objects.equals(pendingBatterRunner.playerId(), batter.playerId())) {
                    clearPendingBatter();
                }
                return;
            }
            pendingBatterRunner = batter;
            pendingBatterTargetBase = targetBase;
        }

        private void placePendingBatterIfPossible() {
            if (pendingBatterRunner == null || pendingBatterTargetBase <= 0) return;
            if (isBaseEmptyOrSameRunner(pendingBatterTargetBase, pendingBatterRunner.playerId())) {
                putRunnerOnBase(pendingBatterTargetBase, pendingBatterRunner);
                clearPendingBatter();
            }
        }

        private void clearPendingBatter() {
            pendingBatterRunner = null;
            pendingBatterTargetBase = 0;
        }

        private boolean isBaseEmptyOrSameRunner(int base, Long playerId) {
            BaseballLiveDTO.BaseRunner current = runnerOnBase(base);
            return current == null || Objects.equals(current.playerId(), playerId);
        }

        private BaseballLiveDTO.BaseRunner runnerOnBase(int base) {
            return switch (base) {
                case 1 -> first;
                case 2 -> second;
                case 3 -> third;
                default -> null;
            };
        }

        private void putRunnerOnBase(int base, BaseballLiveDTO.BaseRunner runner) {
            if (base == 1) first = runner;
            if (base == 2) second = runner;
            if (base == 3) third = runner;
        }

        private void advanceExistingRunner(Long playerId, int bases) {
            if (playerId == null) return;
            BaseballLiveDTO.BaseRunner runner = removeAndReturnRunner(playerId);
            int fromBase = removedRunnerBase;
            if (runner == null) return;
            int targetBase = fromBase + Math.max(1, bases);
            if (targetBase == 2) {
                second = runner;
            } else if (targetBase == 3) {
                third = runner;
            }
        }

        private int removedRunnerBase = 0;

        private BaseballLiveDTO.BaseRunner removeAndReturnRunner(Long playerId) {
            removedRunnerBase = 0;
            BaseballLiveDTO.BaseRunner runner = null;
            if (first != null && Objects.equals(first.playerId(), playerId)) {
                runner = first;
                first = null;
                removedRunnerBase = 1;
            } else if (second != null && Objects.equals(second.playerId(), playerId)) {
                runner = second;
                second = null;
                removedRunnerBase = 2;
            } else if (third != null && Objects.equals(third.playerId(), playerId)) {
                runner = third;
                third = null;
                removedRunnerBase = 3;
            }
            return runner;
        }

        private void moveExistingRunnerTo(Long playerId, int targetBase) {
            if (playerId == null) return;
            BaseballLiveDTO.BaseRunner runner = removeAndReturnRunner(playerId);
            if (runner == null) return;
            if (targetBase == 2) {
                second = runner;
            } else if (targetBase == 3) {
                third = runner;
            }
        }

        private void removeRunner(Long playerId) {
            if (playerId == null) return;
            if (first != null && Objects.equals(first.playerId(), playerId)) first = null;
            if (second != null && Objects.equals(second.playerId(), playerId)) second = null;
            if (third != null && Objects.equals(third.playerId(), playerId)) third = null;
        }

        private void clearBases() {
            first = null;
            second = null;
            third = null;
            clearPendingBatter();
        }

        private BaseballLiveDTO.BaseState baseState() {
            return new BaseballLiveDTO.BaseState(first, second, third);
        }

        private void applyPlayerStats(MatchEvent event, List<MatchLineup> lineups, String type, Map<Long, Player> playerById, Long battingTeamId) {
            Long actorId = event.getPlayerId();
            if ("score".equals(type) && currentAtBat != null) {
                currentAtBat.scoreCount++;
            }
            if (actorId == null && currentAtBat != null && isBatterEvent(type) && !"score".equals(type)) {
                actorId = currentAtBat.batterId;
            }
            if (actorId == null) return;
            Long teamId = event.getTeamId() != null ? event.getTeamId() : battingTeamId;
            if (currentAtBat != null && currentAtBat.batterId == null && isBatterEvent(type) && !"score".equals(type)) {
                currentAtBat.assignBatter(actorId, playerById, lineups);
                lastBatterId = actorId;
            }

            PlayerStatBuilder stat = playerStat(actorId, teamId, playerById, lineups);

            if ("score".equals(type)) {
                stat.runs++;
            }
            if ("stolen_base".equals(type)) stat.steals++;

            if (currentAtBat == null || !Objects.equals(currentAtBat.batterId, actorId)) return;
            if (HIT_TYPES.contains(type)) stat.hits++;
            if ("homerun".equals(type)) stat.homeRuns++;
            if (WALK_TYPES.contains(type)) stat.walks++;
            if (STRIKEOUT_TYPES.contains(type)) stat.strikeouts++;
            if (TERMINAL_TYPES.contains(type) && !currentAtBat.terminalApplied) {
                stat.plateAppearances++;

                if (!Set.of("walk", "intentional_walk", "hit_by_pitch", "sac_bunt", "sac_fly").contains(type)) {
                    stat.atBats++;
                }

                currentAtBat.terminalApplied = true;
                currentAtBat.terminalType = type;
                currentAtBat.resultLabel = eventLabel(type);
                currentAtBat.summary = blankToDefault(event.getDescription(), eventLabel(type));

                currentAtBat.statSnapshot = stat.toDto();
            }
        }

        private void finalizeCurrentAtBatRbi() {
            if (currentAtBat == null || currentAtBat.rbiApplied || currentAtBat.batterId == null) return;
            if (currentAtBat.scoreCount <= 0 || currentAtBat.terminalType == null) return;
            if (!RBI_ALLOWED_TYPES.contains(currentAtBat.terminalType) || RBI_EXCLUDED_TYPES.contains(currentAtBat.terminalType)) return;

            PlayerStatBuilder batterStat = playerStats.get(currentAtBat.batterId);
            if (batterStat != null) {
                batterStat.rbi += currentAtBat.scoreCount;
                currentAtBat.rbiApplied = true;
                currentAtBat.statSnapshot = batterStat.toDto();
            }
        }

        private PlayerStatBuilder playerStat(Long playerId, Long teamId, Map<Long, Player> playerById, List<MatchLineup> lineups) {
            return playerStats.computeIfAbsent(playerId, ignored -> {
                MatchLineup lineup = lineups.stream().filter(row -> Objects.equals(row.getPlayerId(), playerId)).findFirst().orElse(null);
                Player player = playerById.get(playerId);
                return new PlayerStatBuilder(
                        playerId,
                        playerName(player, playerId),
                        teamId,
                        lineup != null ? lineup.getOrderNum() : null,
                        lineup != null ? positionCode(lineup.getPosition()) : (player != null ? positionCode(player.getPosition()) : "-"),
                        seasonStatByPlayerId.get(playerId)
                );
            });
        }

        private void addPitchRow(MatchEvent event, String type) {
            if (currentAtBat == null || Set.of("at_bat_start", "inning_start").contains(type)) return;
            boolean actualPitch = ACTUAL_PITCH_TYPES.contains(type);
            Integer pitchNo = actualPitch ? currentAtBat.nextPitchNo++ : null;
            String count = TERMINAL_TYPES.contains(type) ? "" : countText(balls, strikes);
            currentAtBat.rows.add(new BaseballLiveDTO.PitchRow(
                    pitchNo,
                    event.getPlayerId(),
                    type,
                    eventLabel(type),
                    displayDescription(event, type),
                    count,
                    actualPitch,
                    TERMINAL_TYPES.contains(type)
            ));
        }

        private String displayDescription(MatchEvent event, String type) {
            String description = blankToDefault(event.getDescription(), "");
            Long playerId = event.getPlayerId();
            if (playerId == null) {
                return description;
            }

            String name = playerName(playerById.get(playerId), playerId);
            return switch (type) {
                case "runner_advance" -> runnerAdvanceDescription(description, name, "");
                case "runner_advance_2b" -> runnerAdvanceDescription(description, name, "2\uB8E8");
                case "runner_advance_3b" -> runnerAdvanceDescription(description, name, "3\uB8E8");
                case "score" -> scoreDescription(description, name, playerId);
                case "runner_out", "force_out", "tag_out", "pickoff_out", "caught_stealing" -> outDescription(description, name);
                default -> batterRunnerDescription(description, name);
            };
        }

        private String runnerAdvanceDescription(String description, String name, String fallbackTarget) {
            String text = description == null ? "" : description.trim();
            if (text.contains("1\uB8E8 \uC8FC\uC790")) {
                return text.replaceFirst("1\uB8E8 \uC8FC\uC790", "1\uB8E8 \uC8FC\uC790 " + name);
            }
            if (text.contains("2\uB8E8 \uC8FC\uC790")) {
                return text.replaceFirst("2\uB8E8 \uC8FC\uC790", "2\uB8E8 \uC8FC\uC790 " + name);
            }
            if (text.contains("3\uB8E8 \uC8FC\uC790")) {
                return text.replaceFirst("3\uB8E8 \uC8FC\uC790", "3\uB8E8 \uC8FC\uC790 " + name);
            }
            if (!fallbackTarget.isBlank()) {
                return name + " " + fallbackTarget + "\uAE4C\uC9C0 \uC9C4\uB8E8";
            }
            return name + " \uC9C4\uB8E8";
        }

        private String outDescription(String description, String name) {
            if (description != null && !description.isBlank()) {
                return description;
            }
            return name + " \uC544\uC6C3";
        }

        private String batterRunnerDescription(String description, String name) {
            if (description == null || description.isBlank()) {
                return description;
            }
            if (!description.contains("\uD0C0\uC790\uC8FC\uC790") && !description.contains("\uD0C0\uC790 \uC8FC\uC790")) {
                return description;
            }
            if (description.contains("3\uB8E8")) {
                return name + " 3\uB8E8\uB85C \uCD9C\uB8E8";
            }
            if (description.contains("2\uB8E8")) {
                return name + " 2\uB8E8\uB85C \uCD9C\uB8E8";
            }
            return name + " 1\uB8E8\uB85C \uCD9C\uB8E8";
        }

        private String scoreDescription(String description, String name, Long playerId) {
            String base = scoreBaseFromPriorAdvance(playerId);
            if (description != null) {
                if (base.isBlank() && description.contains("3\uB8E8")) {
                    base = "3\uB8E8\uC8FC\uC790 ";
                } else if (base.isBlank() && description.contains("2\uB8E8")) {
                    base = "2\uB8E8\uC8FC\uC790 ";
                } else if (base.isBlank() && description.contains("1\uB8E8")) {
                    base = "1\uB8E8\uC8FC\uC790 ";
                }
            }
            return base + name + " : \uD648\uC778";
        }

        private String scoreBaseFromPriorAdvance(Long playerId) {
            if (playerId == null || currentAtBat == null) {
                return "";
            }
            for (BaseballLiveDTO.PitchRow row : currentAtBat.rows) {
                if (!Objects.equals(row.playerId(), playerId)) {
                    continue;
                }
                String description = row.description() == null ? "" : row.description();
                if (description.contains("1\uB8E8")) {
                    return "1\uB8E8\uC8FC\uC790 ";
                }
                if (description.contains("2\uB8E8")) {
                    return "2\uB8E8\uC8FC\uC790 ";
                }
                if (description.contains("3\uB8E8")) {
                    return "3\uB8E8\uC8FC\uC790 ";
                }
            }
            return "";
        }

        private BaseballLiveDTO.Scoreboard scoreboard(Match match) {
            TeamTotals home = teamTotals.getOrDefault(match.getHomeTeam().getTeamId(), new TeamTotals());
            TeamTotals away = teamTotals.getOrDefault(match.getAwayTeam().getTeamId(), new TeamTotals());
            int[] homeInnings = inningRunsByTeam.getOrDefault(match.getHomeTeam().getTeamId(), new int[9]);
            int[] awayInnings = inningRunsByTeam.getOrDefault(match.getAwayTeam().getTeamId(), new int[9]);
            List<BaseballLiveDTO.InningScore> inningScores = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                inningScores.add(new BaseballLiveDTO.InningScore(i + 1, homeInnings[i], awayInnings[i]));
            }
            return new BaseballLiveDTO.Scoreboard(
                    home.runs,
                    away.runs,
                    inningScores,
                    home.hits,
                    away.hits,
                    home.errors,
                    away.errors,
                    home.walks,
                    away.walks
            );
        }
    }

    private class AtBatBuilder {
        private final int order;
        private final String period;
        private Long batterId;
        private String batterName;
        private Integer orderNum;
        private String position;
        private final List<BaseballLiveDTO.PitchRow> rows = new ArrayList<>();
        private int nextPitchNo = 1;
        private boolean terminalApplied = false;
        private boolean rbiApplied = false;
        private int scoreCount = 0;
        private String terminalType;
        private String resultLabel = "진행 중";
        private String summary = "진행 중";
        private String battingAvg = "-";
        private BaseballLiveDTO.PlayerGameStat statSnapshot;

        private AtBatBuilder(int order, String period, Long batterId, Map<Long, Player> playerById, List<MatchLineup> lineups) {
            this.order = order;
            this.period = period;
            this.batterId = batterId;
            this.batterName = playerName(playerById.get(batterId), batterId);
            MatchLineup lineup = lineups.stream().filter(row -> Objects.equals(row.getPlayerId(), batterId)).findFirst().orElse(null);
            this.orderNum = lineup != null ? lineup.getOrderNum() : null;
            this.position = lineup != null ? positionCode(lineup.getPosition()) : "-";
        }

        private void assignBatter(Long playerId, Map<Long, Player> playerById, List<MatchLineup> lineups) {
            if (playerId == null || this.batterId != null) return;
            this.batterId = playerId;
            this.batterName = playerName(playerById.get(playerId), playerId);
            MatchLineup lineup = lineups.stream().filter(row -> Objects.equals(row.getPlayerId(), playerId)).findFirst().orElse(null);
            this.orderNum = lineup != null ? lineup.getOrderNum() : null;
            this.position = lineup != null ? positionCode(lineup.getPosition()) : "-";
        }

        private BaseballLiveDTO.AtBatCard toDto(PlayerStatBuilder stat, boolean current) {
            PlayerStatBuilder safeStat = stat != null
                    ? stat
                    : new PlayerStatBuilder(batterId, batterName, null, orderNum, position, null);

            BaseballLiveDTO.PlayerGameStat displayStat;

            if (current) {
                displayStat = safeStat.toDto();
            } else if (statSnapshot != null) {
                displayStat = statSnapshot;
            } else {
                displayStat = safeStat.toDto();
            }

            return new BaseballLiveDTO.AtBatCard(
                    period,
                    batterId,
                    batterName,
                    orderNum,
                    position,
                    current,
                    resultLabel,
                    summary,
                    battingAvg,
                    displayStat,
                    rows
            );
        }
    }

    private class PlayerStatBuilder {
        private final Long playerId;
        private final String name;
        private final Long teamId;
        private final Integer orderNum;
        private final String position;
        private final PlayerSeasonStatBaseball seasonStat;
        private int plateAppearances;
        private int atBats;
        private int hits;
        private int runs;
        private int rbi;
        private int walks;
        private int strikeouts;
        private int steals;
        private int homeRuns;

        private PlayerStatBuilder(Long playerId, String name, Long teamId, Integer orderNum, String position,
                                  PlayerSeasonStatBaseball seasonStat) {
            this.playerId = playerId;
            this.name = name;
            this.teamId = teamId;
            this.orderNum = orderNum;
            this.position = position;
            this.seasonStat = seasonStat;
        }

        private BaseballLiveDTO.PlayerGameStat toDto() {
            return new BaseballLiveDTO.PlayerGameStat(
                    playerId,
                    name,
                    teamId,
                    orderNum,
                    position,
                    plateAppearances,
                    atBats,
                    hits,
                    runs,
                    rbi,
                    walks,
                    strikeouts,
                    steals,
                    homeRuns,
                    battingAverageText(),
                    seasonGamesPlayed(),
                    seasonHits(),
                    seasonHomeRuns(),
                    seasonRbi()
            );
        }

        private String battingAverageText() {
            if (seasonStat == null || seasonStat.getBattingAvg() == null) {
                return "-";
            }
            int baseHits = valueOrZero(seasonStat.getHits());
            int baseAtBats = inferBaseAtBats(baseHits, seasonStat.getBattingAvg());
            int totalAtBats = baseAtBats + atBats;
            if (totalAtBats <= 0) {
                return ".000";
            }
            String text = String.format(Locale.US, "%.3f", (baseHits + hits) / (double) totalAtBats);
            return text.startsWith("0") ? text.substring(1) : text;
        }

        private int inferBaseAtBats(int baseHits, java.math.BigDecimal battingAvg) {
            double avg = battingAvg.doubleValue();
            if (avg <= 0) {
                return baseHits == 0 ? 0 : baseHits;
            }
            return (int) Math.round(baseHits / avg);
        }

        private Integer seasonGamesPlayed() {
            return seasonStat != null && seasonStat.getPlayerSeasonStat() != null
                    ? seasonStat.getPlayerSeasonStat().getGamesPlayed()
                    : null;
        }

        private Integer seasonHits() {
            return seasonStat != null ? seasonStat.getHits() : null;
        }

        private Integer seasonHomeRuns() {
            return seasonStat != null ? seasonStat.getHomeRuns() : null;
        }

        private Integer seasonRbi() {
            return seasonStat != null ? seasonStat.getRbi() : null;
        }
    }

    private class PitcherStatBuilder {
        private final Long playerId;
        private final String name;
        private final Long teamId;
        private final String teamName;
        private final String position;
        private int pitchCount;
        private int hitsAllowed;
        private int strikeouts;
        private int walksAllowed;
        private int hitByPitchAllowed;
        private int runsAllowed;
        private int homeRunsAllowed;
        private int outsPitched;
        private final PlayerSeasonStatBaseball seasonStat;

        private PitcherStatBuilder(Long playerId, String name, Long teamId, String teamName, String position,
                                   PlayerSeasonStatBaseball seasonStat) {
            this.playerId = playerId;
            this.name = name;
            this.teamId = teamId;
            this.teamName = teamName;
            this.position = position;
            this.seasonStat = seasonStat;
        }

        private BaseballLiveDTO.PitcherGameStat toDto() {
            return new BaseballLiveDTO.PitcherGameStat(
                    playerId,
                    name,
                    teamId,
                    teamName,
                    position,
                    inningsPitched(outsPitched),
                    pitchCount,
                    hitsAllowed,
                    strikeouts,
                    walksAllowed,
                    hitByPitchAllowed,
                    runsAllowed,
                    homeRunsAllowed,
                    outsPitched,
                    gameEraText(),
                    seasonEraText(),
                    seasonGamesPlayed(),
                    seasonWins(),
                    seasonLosses(),
                    seasonSaves(),
                    seasonStrikeouts()
            );
        }

        private String gameEraText() {
            if (seasonStat == null || outsPitched <= 0) {
                return "-";
            }
            return String.format(Locale.US, "%.2f", runsAllowed * 27.0 / outsPitched);
        }

        private String seasonEraText() {
            return seasonStat != null && seasonStat.getEra() != null ? seasonStat.getEra().toPlainString() : "-";
        }

        private Integer seasonGamesPlayed() {
            return seasonStat != null && seasonStat.getPlayerSeasonStat() != null
                    ? seasonStat.getPlayerSeasonStat().getGamesPlayed()
                    : null;
        }

        private Integer seasonWins() {
            return seasonStat != null ? seasonStat.getWins() : null;
        }

        private Integer seasonLosses() {
            return seasonStat != null ? seasonStat.getLosses() : null;
        }

        private Integer seasonSaves() {
            return seasonStat != null ? seasonStat.getSaves() : null;
        }

        private Integer seasonStrikeouts() {
            return seasonStat != null ? seasonStat.getStrikeouts() : null;
        }
    }

    private static class TeamTotals {
        private int runs;
        private int hits;
        private int errors;
        private int walks;
    }
}

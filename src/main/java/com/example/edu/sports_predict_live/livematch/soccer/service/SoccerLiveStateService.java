package com.example.edu.sports_predict_live.livematch.soccer.service;

import com.example.edu.sports_predict_live.livematch.event.entity.MatchEvent;
import com.example.edu.sports_predict_live.livematch.event.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.livematch.lineup.entity.MatchLineup;
import com.example.edu.sports_predict_live.livematch.lineup.repository.MatchLineupRepository;
import com.example.edu.sports_predict_live.livematch.soccer.dto.SoccerLiveDTO;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.prediction.service.PredictionService;
import com.example.edu.sports_predict_live.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoccerLiveStateService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, List<String>> FORMATION_SLOTS = Map.of(
            "4-4-2", List.of("GK", "LB", "CB", "CB", "RB", "LM", "CM", "CM", "RM", "ST", "ST"),
            "4-3-3", List.of("GK", "LB", "CB", "CB", "RB", "CM", "CM", "CM", "LW", "ST", "RW"),
            "4-2-3-1", List.of("GK", "LB", "CB", "CB", "RB", "DM", "DM", "AM", "LW", "RW", "ST")
    );

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final PlayerRepository playerRepository;
    private final PredictionService predictionService;

    public SoccerLiveDTO getSoccerLive(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        List<MatchEvent> events = matchEventRepository.findByMatchIdOrderByEventTimeAscMatchEventIdAsc(matchId);
        List<MatchLineup> lineups = matchLineupRepository.findByMatchIdOrderByTeamIdAscStarterDescOrderNumAscMatchLineupIdAsc(matchId);
        Map<Long, Player> playerById = loadPlayerMap(events, lineups);
        State state = buildState(match, events, lineups, playerById);

        return new SoccerLiveDTO(
                match.getMatchId(),
                "soccer",
                match.getStatus(),
                match.getScheduledAt(),
                scoreboard(match, state),
                timeline(state),
                records(match, lineups, playerById, state),
                lineup(match, lineups, playerById),
                predictionService.getMatchSummary(matchId),
                List.of()
        );
    }

    private State buildState(Match match, List<MatchEvent> events, List<MatchLineup> lineups, Map<Long, Player> playerById) {
        State state = new State(match);
        for (MatchLineup lineup : lineups) {
            state.player(lineup.getPlayerId(), lineup.getTeamId(), playerById.get(lineup.getPlayerId()), lineup);
        }

        for (MatchEvent event : events) {
            String type = type(event);
            String period = period(event);
            int minute = minute(event);
            Long teamId = event.getTeamId();
            Long playerId = event.getPlayerId();
            TeamStatBuilder team = state.team(teamId);
            PlayerStatBuilder player = state.player(playerId, teamId, playerById.get(playerId), findLineup(lineups, playerId));

            if ("match_start".equals(type)) {
                state.phase = "전반";
                state.currentMinute = "0";
            } else if ("first_half_end".equals(type)) {
                state.phase = "하프타임";
                state.currentMinute = "45";
            } else if ("second_half_start".equals(type)) {
                state.phase = "후반";
                state.currentMinute = "46";
            } else if ("match_end".equals(type)) {
                state.phase = "경기 종료";
                state.currentMinute = "90";
            } else if (minute > 0) {
                state.currentMinute = minuteText(minute);
                state.phase = "second_half".equals(period) ? "후반" : "전반";
            }

            switch (type) {
                case "goal", "penalty_goal" -> {
                    if (Objects.equals(teamId, match.getHomeTeam().getTeamId())) state.homeScore++;
                    if (Objects.equals(teamId, match.getAwayTeam().getTeamId())) state.awayScore++;
                    if (team != null) team.goals++;
                    if (player != null) player.goals++;
                }
                case "own_goal" -> {
                    if (Objects.equals(teamId, match.getHomeTeam().getTeamId())) state.awayScore++;
                    if (Objects.equals(teamId, match.getAwayTeam().getTeamId())) state.homeScore++;
                }
                case "shot" -> {
                    if (team != null) team.shots++;
                    if (player != null) player.shots++;
                }
                case "shot_on_target" -> {
                    if (team != null) {
                        team.shots++;
                        team.shotsOnTarget++;
                    }
                    if (player != null) {
                        player.shots++;
                        player.shotsOnTarget++;
                    }
                }
                case "corner_kick" -> {
                    if (team != null) team.cornerKicks++;
                }
                case "foul" -> {
                    if (team != null) team.fouls++;
                    if (player != null) player.fouls++;
                }
                case "yellow_card" -> {
                    if (team != null) team.yellowCards++;
                    if (player != null) player.yellowCards++;
                }
                case "red_card" -> {
                    if (team != null) team.redCards++;
                    if (player != null) player.redCards++;
                }
                case "offside" -> {
                    if (team != null) team.offsides++;
                    if (player != null) player.offsides++;
                }
                case "save" -> {
                    if (team != null) team.saves++;
                    if (player != null) player.saves++;
                }
                default -> {
                }
            }

            state.events.add(toTimelineEvent(match, event, playerById, state.homeScore, state.awayScore));
        }
        return state;
    }

    private Map<Long, Player> loadPlayerMap(List<MatchEvent> events, List<MatchLineup> lineups) {
        Set<Long> ids = new HashSet<>();
        events.stream().map(MatchEvent::getPlayerId).filter(Objects::nonNull).forEach(ids::add);
        lineups.stream().map(MatchLineup::getPlayerId).filter(Objects::nonNull).forEach(ids::add);
        return playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getPlayerId, player -> player));
    }

    private SoccerLiveDTO.Scoreboard scoreboard(Match match, State state) {
        return new SoccerLiveDTO.Scoreboard(
                match.getHomeTeam().getTeamId(),
                match.getAwayTeam().getTeamId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeTeam().getEmblemUrl(),
                match.getAwayTeam().getEmblemUrl(),
                state.homeScore,
                state.awayScore,
                match.getVenue(),
                match.getScheduledAt().format(DATE_FORMAT),
                match.getScheduledAt().format(TIME_FORMAT),
                state.currentMinute,
                state.phase,
                state.formation(match.getHomeTeam().getTeamId()),
                state.formation(match.getAwayTeam().getTeamId())
        );
    }

    private SoccerLiveDTO.Timeline timeline(State state) {
        Map<String, List<SoccerLiveDTO.TimelineEvent>> grouped = state.events.stream()
                .sorted(Comparator.comparing(SoccerLiveDTO.TimelineEvent::eventId).reversed())
                .collect(Collectors.groupingBy(
                        event -> sectionKey(event.minute()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<SoccerLiveDTO.TimelineSection> sections = new ArrayList<>();
        sections.add(section("second_half", "후반", grouped.getOrDefault("second_half", List.of()), true));
        sections.add(section("first_half", "전반", grouped.getOrDefault("first_half", List.of()), sections.get(0).events().isEmpty()));
        return new SoccerLiveDTO.Timeline(sections);
    }

    private SoccerLiveDTO.TimelineSection section(String key, String title, List<SoccerLiveDTO.TimelineEvent> events, boolean open) {
        String scoreText = events.isEmpty() ? "-" : events.get(0).awayScore() + " - " + events.get(0).homeScore();
        return new SoccerLiveDTO.TimelineSection(key, title, scoreText, open, events);
    }

    private SoccerLiveDTO.Records records(Match match, List<MatchLineup> lineups, Map<Long, Player> playerById, State state) {
        Long homeId = match.getHomeTeam().getTeamId();
        Long awayId = match.getAwayTeam().getTeamId();
        List<SoccerLiveDTO.PlayerStat> home = state.players.values().stream()
                .filter(stat -> Objects.equals(stat.teamId, homeId))
                .sorted(playerStatOrder())
                .map(stat -> stat.toDto(state.elapsedMinute()))
                .toList();
        List<SoccerLiveDTO.PlayerStat> away = state.players.values().stream()
                .filter(stat -> Objects.equals(stat.teamId, awayId))
                .sorted(playerStatOrder())
                .map(stat -> stat.toDto(state.elapsedMinute()))
                .toList();
        return new SoccerLiveDTO.Records(
                new SoccerLiveDTO.TeamRecords(state.team(homeId).toDto(), state.team(awayId).toDto()),
                new SoccerLiveDTO.PlayerRecords(home, away)
        );
    }

    private Comparator<PlayerStatBuilder> playerStatOrder() {
        return Comparator.comparing((PlayerStatBuilder stat) -> stat.starter ? 0 : 1)
                .thenComparing(stat -> stat.orderNum == null ? 999 : stat.orderNum)
                .thenComparing(stat -> stat.playerName == null ? "" : stat.playerName);
    }

    private SoccerLiveDTO.Lineup lineup(Match match, List<MatchLineup> lineups, Map<Long, Player> playerById) {
        return new SoccerLiveDTO.Lineup(
                lineupTeam(match.getHomeTeam(), lineups, playerById),
                lineupTeam(match.getAwayTeam(), lineups, playerById)
        );
    }

    private SoccerLiveDTO.LineupTeam lineupTeam(Team team, List<MatchLineup> lineups, Map<Long, Player> playerById) {
        List<SoccerLiveDTO.LineupPlayer> rows = lineups.stream()
                .filter(lineup -> Objects.equals(lineup.getTeamId(), team.getTeamId()))
                .sorted(Comparator.comparing((MatchLineup lineup) -> lineup.isStarter() ? 0 : 1)
                        .thenComparing(lineup -> lineup.getOrderNum() == null ? 999 : lineup.getOrderNum()))
                .map(lineup -> {
                    Player player = playerById.get(lineup.getPlayerId());
                    return new SoccerLiveDTO.LineupPlayer(
                            lineup.getPlayerId(),
                            playerName(player, lineup.getPlayerId()),
                            lineup.getOrderNum(),
                            position(lineup.getPosition(), player),
                            lineup.isStarter(),
                            player != null ? player.getJerseyNumber() : null
                    );
                })
                .toList();
        return new SoccerLiveDTO.LineupTeam(
                team.getTeamId(),
                team.getName(),
                team.getEmblemUrl(),
                formation(rows),
                rows.stream().filter(SoccerLiveDTO.LineupPlayer::starter).toList(),
                rows.stream().filter(player -> !player.starter()).toList()
        );
    }

    private SoccerLiveDTO.TimelineEvent toTimelineEvent(Match match, MatchEvent event, Map<Long, Player> playerById, int homeScore, int awayScore) {
        Team team = null;
        if (Objects.equals(event.getTeamId(), match.getHomeTeam().getTeamId())) team = match.getHomeTeam();
        if (Objects.equals(event.getTeamId(), match.getAwayTeam().getTeamId())) team = match.getAwayTeam();
        Player player = playerById.get(event.getPlayerId());
        String type = type(event);
        return new SoccerLiveDTO.TimelineEvent(
                event.getMatchEventId(),
                type,
                label(type),
                icon(type),
                minuteText(minute(event)),
                event.getTeamId(),
                team != null ? team.getName() : null,
                event.getPlayerId(),
                playerName(player, event.getPlayerId()),
                event.getDescription(),
                homeScore,
                awayScore
        );
    }

    private MatchLineup findLineup(List<MatchLineup> lineups, Long playerId) {
        if (playerId == null) return null;
        return lineups.stream().filter(lineup -> Objects.equals(lineup.getPlayerId(), playerId)).findFirst().orElse(null);
    }

    private String type(MatchEvent event) {
        return event.getEventType() == null ? "" : event.getEventType().trim().toLowerCase(Locale.ROOT);
    }

    private String period(MatchEvent event) {
        String value = event.getEventPeriod();
        if (value == null || value.isBlank()) return minute(event) > 45 ? "second_half" : "first_half";
        return value.trim();
    }

    private int minute(MatchEvent event) {
        return event.getEventTime() == null ? 0 : Math.max(0, event.getEventTime());
    }

    private String minuteText(int minute) {
        if (minute <= 0) return "0";
        if (minute > 90) return "90+" + (minute - 90);
        return String.valueOf(minute);
    }

    private String sectionKey(String minute) {
        if (minute == null || minute.isBlank()) return "first_half";
        String base = minute.split("\\+")[0];
        try {
            return Integer.parseInt(base) > 45 ? "second_half" : "first_half";
        } catch (NumberFormatException e) {
            return "first_half";
        }
    }

    private String label(String type) {
        return switch (type) {
            case "match_start" -> "경기 시작";
            case "first_half_end" -> "전반 종료";
            case "second_half_start" -> "후반 시작";
            case "match_end" -> "경기 종료";
            case "goal" -> "골";
            case "own_goal" -> "자책골";
            case "penalty_goal" -> "PK 골";
            case "penalty_miss" -> "PK 실축";
            case "shot" -> "슈팅";
            case "shot_on_target" -> "유효슈팅";
            case "corner_kick" -> "코너킥";
            case "foul" -> "파울";
            case "yellow_card" -> "경고";
            case "red_card" -> "퇴장";
            case "offside" -> "오프사이드";
            case "save" -> "선방";
            case "substitution" -> "교체";
            default -> type;
        };
    }

    private String icon(String type) {
        return switch (type) {
            case "goal", "penalty_goal" -> "G";
            case "own_goal" -> "OG";
            case "penalty_miss" -> "PK";
            case "shot", "shot_on_target" -> "S";
            case "yellow_card" -> "YC";
            case "red_card" -> "RC";
            case "substitution" -> "SUB";
            default -> "•";
        };
    }

    private String playerName(Player player, Long playerId) {
        if (player != null && player.getName() != null) return player.getName();
        return playerId == null ? "-" : "Player " + playerId;
    }

    private String position(String raw, Player player) {
        if (raw != null && !raw.isBlank()) return raw.trim().toUpperCase(Locale.ROOT);
        if (player != null && player.getPosition() != null) return player.getPosition().trim().toUpperCase(Locale.ROOT);
        return "-";
    }

    private String formation(List<SoccerLiveDTO.LineupPlayer> rows) {
        List<SoccerLiveDTO.LineupPlayer> starters = rows.stream()
                .filter(SoccerLiveDTO.LineupPlayer::starter)
                .toList();

        Map<Integer, String> bySlot = new HashMap<>();
        for (SoccerLiveDTO.LineupPlayer player : starters) {
            Integer orderNum = player.orderNum();
            if (orderNum == null || orderNum < 1 || orderNum > 11) continue;
            bySlot.putIfAbsent(orderNum, normalizeSoccerPosition(player.position()));
        }

        if (!bySlot.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : FORMATION_SLOTS.entrySet()) {
                if (matchesFormation(bySlot, entry.getValue())) {
                    return entry.getKey();
                }
            }
        }

        List<String> positions = starters.stream()
                .map(SoccerLiveDTO.LineupPlayer::position)
                .filter(Objects::nonNull)
                .map(this::normalizeSoccerPosition)
                .toList();
        long defenders = positions.stream().filter(Set.of("LB", "CB", "RB", "LWB", "RWB", "DF")::contains).count();
        long mids = positions.stream().filter(Set.of("DM", "CM", "AM", "LM", "RM", "MF")::contains).count();
        long attackers = positions.stream().filter(Set.of("ST", "FW", "LW", "RW")::contains).count();

        if (defenders == 4
                && positions.stream().filter("DM"::equals).count() >= 2
                && positions.contains("AM")
                && positions.contains("ST")) {
            return "4-2-3-1";
        }
        if (defenders == 4 && mids == 4 && attackers == 2) return "4-4-2";
        if (defenders == 4 && mids == 3 && attackers == 3) return "4-3-3";
        if (defenders == 4 && mids == 5 && attackers == 1) return "4-2-3-1";
        return "4-4-2";
    }

    private boolean matchesFormation(Map<Integer, String> bySlot, List<String> expectedSlots) {
        for (int i = 0; i < expectedSlots.size(); i++) {
            String actual = bySlot.get(i + 1);
            if (actual == null) continue;
            String expected = expectedSlots.get(i);
            if (!sameSlot(actual, expected)) return false;
        }
        return true;
    }

    private boolean sameSlot(String actual, String expected) {
        String a = normalizeSoccerPosition(actual);
        String e = normalizeSoccerPosition(expected);
        if (Objects.equals(a, e)) return true;
        if ((Objects.equals(a, "LW") || Objects.equals(a, "LM")) && (Objects.equals(e, "LW") || Objects.equals(e, "LM"))) return true;
        if ((Objects.equals(a, "RW") || Objects.equals(a, "RM")) && (Objects.equals(e, "RW") || Objects.equals(e, "RM"))) return true;
        if ((Objects.equals(a, "ST") || Objects.equals(a, "FW")) && (Objects.equals(e, "ST") || Objects.equals(e, "FW"))) return true;
        return false;
    }

    private String normalizeSoccerPosition(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FWD", "CF", "ATT" -> "ST";
            case "MID" -> "CM";
            case "DEF" -> "CB";
            case "LWF" -> "LW";
            case "RWF" -> "RW";
            default -> value;
        };
    }

    private class State {
        private int homeScore;
        private int awayScore;
        private String currentMinute = "0";
        private String phase = "경기전";
        private final Map<Long, TeamStatBuilder> teams = new HashMap<>();
        private final Map<Long, PlayerStatBuilder> players = new HashMap<>();
        private final List<SoccerLiveDTO.TimelineEvent> events = new ArrayList<>();
        private final Match match;

        private State(Match match) {
            this.match = match;
            this.homeScore = 0;
            this.awayScore = 0;
            teams.put(match.getHomeTeam().getTeamId(), new TeamStatBuilder(match.getHomeTeam()));
            teams.put(match.getAwayTeam().getTeamId(), new TeamStatBuilder(match.getAwayTeam()));
        }

        private TeamStatBuilder team(Long teamId) {
            if (teamId == null) return null;
            return teams.computeIfAbsent(teamId, ignored -> new TeamStatBuilder(teamId, null));
        }

        private PlayerStatBuilder player(Long playerId, Long teamId, Player player, MatchLineup lineup) {
            if (playerId == null) return null;
            Long resolvedTeamId = teamId != null ? teamId : player != null && player.getTeam() != null ? player.getTeam().getTeamId() : null;
            return players.computeIfAbsent(playerId, ignored -> new PlayerStatBuilder(playerId, resolvedTeamId, player, lineup));
        }

        private String formation(Long teamId) {
            List<SoccerLiveDTO.LineupPlayer> rows = players.values().stream()
                    .filter(player -> Objects.equals(player.teamId, teamId))
                    .map(PlayerStatBuilder::toLineupPlayer)
                    .toList();
            return SoccerLiveStateService.this.formation(rows);
        }

        private int elapsedMinute() {
            if (currentMinute == null || currentMinute.isBlank()) return 0;
            String[] parts = currentMinute.split("\\+");
            try {
                int base = Integer.parseInt(parts[0].trim());
                int added = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                return Math.max(0, Math.min(130, base + added));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    private static class TeamStatBuilder {
        private final Long teamId;
        private final String teamName;
        private int goals;
        private int passes;
        private int shots;
        private int shotsOnTarget;
        private int cornerKicks;
        private int fouls;
        private int offsides;
        private int yellowCards;
        private int redCards;
        private int saves;

        private TeamStatBuilder(Team team) {
            this(team.getTeamId(), team.getName());
        }

        private TeamStatBuilder(Long teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName;
        }

        private SoccerLiveDTO.TeamStat toDto() {
            return new SoccerLiveDTO.TeamStat(teamId, teamName, goals, passes, shots, shotsOnTarget, cornerKicks, fouls, offsides, yellowCards, redCards, saves);
        }
    }

    private class PlayerStatBuilder {
        private final Long playerId;
        private final String playerName;
        private final Long teamId;
        private final String teamName;
        private final Integer orderNum;
        private final String position;
        private final boolean starter;
        private final Integer jerseyNumber;
        private int goals;
        private int assists;
        private int passes;
        private int shots;
        private int shotsOnTarget;
        private int fouls;
        private int offsides;
        private int yellowCards;
        private int redCards;
        private int saves;

        private PlayerStatBuilder(Long playerId, Long teamId, Player player, MatchLineup lineup) {
            this.playerId = playerId;
            this.playerName = playerName(player, playerId);
            this.teamId = teamId;
            this.teamName = player != null && player.getTeam() != null ? player.getTeam().getName() : null;
            this.orderNum = lineup != null ? lineup.getOrderNum() : null;
            this.position = position(lineup != null ? lineup.getPosition() : null, player);
            this.starter = lineup != null && lineup.isStarter();
            this.jerseyNumber = player != null ? player.getJerseyNumber() : null;
        }

        private SoccerLiveDTO.PlayerStat toDto(int elapsedMinute) {
            int minutes = starter ? Math.min(90, Math.max(0, elapsedMinute)) : 0;
            return new SoccerLiveDTO.PlayerStat(playerId, playerName, teamId, teamName, orderNum, position, starter, minutes,
                    goals, assists, passes, shots, shotsOnTarget, fouls, offsides, yellowCards, redCards, saves);
        }

        private SoccerLiveDTO.LineupPlayer toLineupPlayer() {
            return new SoccerLiveDTO.LineupPlayer(playerId, playerName, orderNum, position, starter, jerseyNumber);
        }
    }
}

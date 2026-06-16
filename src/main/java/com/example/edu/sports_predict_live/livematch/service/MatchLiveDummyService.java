package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchLiveDTO;
import com.example.edu.sports_predict_live.livematch.entity.MatchEvent;
import com.example.edu.sports_predict_live.livematch.repository.MatchEventRepository;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MatchLiveDummyService {

    private static final String CURRENT_SEASON = "2026";
    private static final DateTimeFormatter SCHEDULE_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final Pattern COUNT_PATTERN = Pattern.compile("B:(\\d+)\\s+S:(\\d+)\\s+O:(\\d+)");
    private static final Pattern RUNNER_PATTERN = Pattern.compile("R:([0-9,·-]+)");
    private static final Pattern INNING_PATTERN = Pattern.compile("(\\d+)회(초|말)");

    private final MatchRepository matchRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerSeasonStatBaseballRepository baseballRecordRepository;

    @Transactional(readOnly = true)
    public MatchLiveDTO getBaseballLive(Long matchId) {
        return matchRepository.findById(matchId)
                .map(this::toLiveDto)
                .orElseGet(() -> fallbackLiveDto(matchId));
    }

    private MatchLiveDTO toLiveDto(Match match) {
        List<PlayerSeasonStatBaseball> hitters = baseballRecordRepository.findHittersBySportAndSeason("baseball", CURRENT_SEASON);
        List<PlayerSeasonStatBaseball> pitchers = baseballRecordRepository.findPitchersBySportAndSeason("baseball", CURRENT_SEASON);
        EventContext eventContext = buildEventContext(match.getMatchId());

        PlayerSeasonStatBaseball homeBatter = firstByTeam(hitters, match.getHomeTeam().getTeamId());
        PlayerSeasonStatBaseball awayBatter = firstByTeam(hitters, match.getAwayTeam().getTeamId());
        PlayerSeasonStatBaseball homePitcher = firstByTeam(pitchers, match.getHomeTeam().getTeamId());
        PlayerSeasonStatBaseball awayPitcher = firstByTeam(pitchers, match.getAwayTeam().getTeamId());

        String status = normalizeStatus(match.getStatus());
        MatchLiveDTO.CurrentPlayer currentBatter = "finished".equals(status)
                ? currentPlayer(awayBatter, match.getAwayTeam().getName(), "last batter candidate")
                : currentPlayer(homeBatter, match.getHomeTeam().getName(), "current batter candidate");
        MatchLiveDTO.CurrentPlayer currentPitcher = "scheduled".equals(status)
                ? currentPlayer(homePitcher, match.getHomeTeam().getName(), "probable starter")
                : currentPlayer(awayPitcher, match.getAwayTeam().getName(), "current pitcher candidate");

        if (eventContext.lastPlayer() != null && eventContext.lastTeam() != null) {
            currentBatter = currentPlayerFromEvent(eventContext.lastPlayer(), eventContext.lastTeam());
        }

        LiveState state = liveState(match, status, currentBatter, currentPitcher, eventContext);
        String homeShortName = shortName(match.getHomeTeam().getName());
        String awayShortName = shortName(match.getAwayTeam().getName());

        return new MatchLiveDTO(
                match.getMatchId(),
                match.getSport().getCode(),
                status,
                state.currentInning(),
                new MatchLiveDTO.TeamScore(
                        match.getHomeTeam().getTeamId(),
                        match.getHomeTeam().getName(),
                        homeShortName,
                        logoText(match.getHomeTeam().getName()),
                        "home"
                ),
                new MatchLiveDTO.TeamScore(
                        match.getAwayTeam().getTeamId(),
                        match.getAwayTeam().getName(),
                        awayShortName,
                        logoText(match.getAwayTeam().getName()),
                        "away"
                ),
                new MatchLiveDTO.Score(match.getHomeScore(), match.getAwayScore()),
                currentBatter,
                currentPitcher,
                state.count(),
                state.runners(),
                inningScores(match, status, homeShortName, awayShortName, eventContext.rawEvents()),
                state.events(),
                comments(match, state, eventContext),
                prediction(match, status),
                new MatchLiveDTO.ViewerState(false, false, false)
        );
    }

    private EventContext buildEventContext(Long matchId) {
        List<MatchEvent> events = matchEventRepository.findByMatchIdOrderByEventTimeAscMatchEventIdAsc(matchId);
        if (events.isEmpty()) {
            return EventContext.empty();
        }

        Set<Long> playerIds = new HashSet<>();
        Set<Long> teamIds = new HashSet<>();
        for (MatchEvent event : events) {
            if (event.getPlayerId() != null) playerIds.add(event.getPlayerId());
            if (event.getTeamId() != null) teamIds.add(event.getTeamId());
        }

        Map<Long, Player> playerById = new HashMap<>();
        playerRepository.findAllById(playerIds).forEach(player -> playerById.put(player.getPlayerId(), player));

        Map<Long, Team> teamById = new HashMap<>();
        teamRepository.findAllById(teamIds).forEach(team -> teamById.put(team.getTeamId(), team));

        List<MatchLiveDTO.LiveEvent> liveEvents = events.stream()
                .map(event -> toLiveEvent(event, playerById, teamById))
                .toList();

        MatchEvent lastEvent = events.get(events.size() - 1);
        MatchLiveDTO.Count count = parseCount(lastEvent.getDescription());
        MatchLiveDTO.Runners runners = parseRunners(lastEvent.getDescription());
        String currentInning = valueOrDefault(lastEvent.getEventPeriod(), "LIVE");
        Player lastPlayer = lastEvent.getPlayerId() == null ? null : playerById.get(lastEvent.getPlayerId());
        Team lastTeam = lastEvent.getTeamId() == null ? null : teamById.get(lastEvent.getTeamId());

        return new EventContext(events, liveEvents, count, runners, currentInning, lastPlayer, lastTeam);
    }

    private MatchLiveDTO.LiveEvent toLiveEvent(MatchEvent event, Map<Long, Player> playerById, Map<Long, Team> teamById) {
        Player player = event.getPlayerId() == null ? null : playerById.get(event.getPlayerId());
        Team team = event.getTeamId() == null ? null : teamById.get(event.getTeamId());
        String eventType = event.getEventType();
        String playerName = player == null ? null : player.getName();
        String teamName = team == null ? null : team.getName();
        String title = playerName == null
                ? eventTypeLabel(eventType)
                : playerName + " " + eventTypeLabel(eventType);
        String detail = cleanDescription(event.getDescription());
        if (teamName != null && !detail.contains(teamName)) {
            detail = teamName + " · " + detail;
        }
        return new MatchLiveDTO.LiveEvent(
                eventType,
                event.getEventTime() == null ? 0 : event.getEventTime(),
                valueOrDefault(event.getEventPeriod(), "-"),
                title,
                detail
        );
    }

    private LiveState liveState(Match match, String status, MatchLiveDTO.CurrentPlayer batter, MatchLiveDTO.CurrentPlayer pitcher, EventContext eventContext) {
        if (!eventContext.events().isEmpty() && !"scheduled".equals(status) && !"cancelled".equals(status)) {
            return new LiveState(
                    eventContext.currentInning(),
                    eventContext.count(),
                    eventContext.runners(),
                    eventContext.events()
            );
        }
        return switch (status) {
            case "in_progress" -> inProgressState(match, batter, pitcher);
            case "finished" -> finishedState(match, batter, pitcher);
            case "cancelled" -> cancelledState(match);
            case "paused" -> pausedState(batter, pitcher);
            default -> scheduledState(match, batter, pitcher);
        };
    }

    private LiveState scheduledState(Match match, MatchLiveDTO.CurrentPlayer batter, MatchLiveDTO.CurrentPlayer pitcher) {
        String time = match.getScheduledAt().format(SCHEDULE_FMT);
        return new LiveState(
                "PRE",
                new MatchLiveDTO.Count(0, 0, 0),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.LiveEvent("match_loaded", 0, "PREVIEW", "DB schedule loaded", match.getAwayTeam().getName() + " vs " + match.getHomeTeam().getName()),
                        new MatchLiveDTO.LiveEvent("scheduled_at", 0, "START", "Scheduled time", time),
                        new MatchLiveDTO.LiveEvent("venue", 0, "VENUE", "Ballpark", valueOrDefault(match.getVenue(), "venue unknown")),
                        new MatchLiveDTO.LiveEvent("pitcher_record", 0, "PROBABLE", pitcher.name(), pitcher.description()),
                        new MatchLiveDTO.LiveEvent("batter_record", 0, "KEY HITTER", batter.name(), batter.description())
                )
        );
    }

    private LiveState inProgressState(Match match, MatchLiveDTO.CurrentPlayer batter, MatchLiveDTO.CurrentPlayer pitcher) {
        return new LiveState(
                "LIVE",
                new MatchLiveDTO.Count(0, 0, 0),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.LiveEvent("result", 0, "LIVE", "이벤트 없음", "match_event에 이 경기의 중계 이벤트가 아직 없습니다."),
                        new MatchLiveDTO.LiveEvent("at_bat_start", 0, "LIVE", batter.name(), pitcher.name() + " 상대 타석 대기")
                )
        );
    }

    private LiveState pausedState(MatchLiveDTO.CurrentPlayer batter, MatchLiveDTO.CurrentPlayer pitcher) {
        return new LiveState(
                "PAUSED",
                new MatchLiveDTO.Count(0, 0, 0),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.LiveEvent("result", 0, "PAUSED", "경기 일시중지", batter.name() + " / " + pitcher.name())
                )
        );
    }

    private LiveState finishedState(Match match, MatchLiveDTO.CurrentPlayer batter, MatchLiveDTO.CurrentPlayer pitcher) {
        String winner = match.getHomeScore() >= match.getAwayScore() ? match.getHomeTeam().getName() : match.getAwayTeam().getName();
        return new LiveState(
                "FINAL",
                new MatchLiveDTO.Count(0, 0, 3),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.LiveEvent("result", 0, "FINAL", "Game finished", winner + " wins"),
                        new MatchLiveDTO.LiveEvent("result", 0, "Final score", match.getAwayTeam().getName() + " " + match.getAwayScore() + " : " + match.getHomeScore() + " " + match.getHomeTeam().getName(), "DB 경기 결과")
                )
        );
    }

    private LiveState cancelledState(Match match) {
        return new LiveState(
                "CANCELLED",
                new MatchLiveDTO.Count(0, 0, 0),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.LiveEvent("result", 0, "CANCELLED", "Schedule status changed", match.getAwayTeam().getName() + " vs " + match.getHomeTeam().getName()),
                        new MatchLiveDTO.LiveEvent("venue", 0, "VENUE", "Cancelled game", valueOrDefault(match.getVenue(), "venue unknown")),
                        new MatchLiveDTO.LiveEvent("scheduled_at", 0, "ORIGINAL START", "Scheduled time", match.getScheduledAt().format(SCHEDULE_FMT))
                )
        );
    }

    private List<MatchLiveDTO.InningScore> inningScores(Match match, String status, String homeShortName, String awayShortName, List<MatchEvent> events) {
        if ("scheduled".equals(status) || "cancelled".equals(status)) {
            return List.of(
                    new MatchLiveDTO.InningScore(awayShortName, List.of("-", "-", "-", "-", "-", "-", "-", "-", "-"), 0, 0, 0, 0),
                    new MatchLiveDTO.InningScore(homeShortName, List.of("-", "-", "-", "-", "-", "-", "-", "-", "-"), 0, 0, 0, 0)
            );
        }

        int inningCount = Math.max(9, events.stream()
                .map(MatchEvent::getEventPeriod)
                .mapToInt(this::inningNumber)
                .max()
                .orElse(9));
        int[] awayRuns = new int[inningCount];
        int[] homeRuns = new int[inningCount];
        int awayHits = 0;
        int homeHits = 0;
        int awayWalks = 0;
        int homeWalks = 0;
        int awayErrors = 0;
        int homeErrors = 0;

        for (MatchEvent event : events) {
            boolean awaySide = match.getAwayTeam().getTeamId().equals(event.getTeamId());
            boolean homeSide = match.getHomeTeam().getTeamId().equals(event.getTeamId());
            int inningIndex = inningIndex(event.getEventPeriod());
            String type = String.valueOf(event.getEventType()).toLowerCase();

            if ("score".equals(type) && inningIndex >= 0 && inningIndex < inningCount) {
                if (awaySide) awayRuns[inningIndex] += 1;
                if (homeSide) homeRuns[inningIndex] += 1;
            }
            if (List.of("hit", "single", "double", "triple", "homerun").contains(type)) {
                if (awaySide) awayHits += 1;
                if (homeSide) homeHits += 1;
            }
            if ("walk".equals(type)) {
                if (awaySide) awayWalks += 1;
                if (homeSide) homeWalks += 1;
            }
            if ("error".equals(type)) {
                if (awaySide) awayErrors += 1;
                if (homeSide) homeErrors += 1;
            }
        }

        return List.of(
                new MatchLiveDTO.InningScore(awayShortName, inningList(awayRuns), match.getAwayScore(), awayHits, awayErrors, awayWalks),
                new MatchLiveDTO.InningScore(homeShortName, inningList(homeRuns), match.getHomeScore(), homeHits, homeErrors, homeWalks)
        );
    }

    private List<MatchLiveDTO.CommentPreview> comments(Match match, LiveState state, EventContext eventContext) {
        if (!eventContext.events().isEmpty()) {
            return List.of(
                    new MatchLiveDTO.CommentPreview("system", "match_event " + eventContext.events().size() + "건을 DB에서 불러왔습니다.", "now"),
                    new MatchLiveDTO.CommentPreview("scorebook", state.currentInning() + " 기준 화면을 갱신했습니다.", "now")
            );
        }
        return switch (state.currentInning()) {
            case "PRE" -> List.of(new MatchLiveDTO.CommentPreview("system", "경기 시작 전입니다. 일정/팀/선수 DB만 표시합니다.", "now"));
            case "FINAL" -> List.of(new MatchLiveDTO.CommentPreview("system", "종료 경기입니다. match_event가 없으면 상세 중계는 표시하지 않습니다.", "now"));
            case "CANCELLED" -> List.of(new MatchLiveDTO.CommentPreview("system", "취소 경기입니다.", "now"));
            default -> List.of(new MatchLiveDTO.CommentPreview("system", "match_event에 이 경기의 이벤트를 넣으면 중계 타임라인이 채워집니다.", "now"));
        };
    }

    private MatchLiveDTO.PredictionPreview prediction(Match match, String status) {
        int homePercent = Math.max(35, Math.min(65, 50 + match.getHomeScore() - match.getAwayScore()));
        return new MatchLiveDTO.PredictionPreview(
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                homePercent,
                100 - homePercent,
                0,
                0,
                !"scheduled".equals(status)
        );
    }

    private PlayerSeasonStatBaseball firstByTeam(List<PlayerSeasonStatBaseball> records, Long teamId) {
        return records.stream()
                .filter(stat -> stat.getPlayerSeasonStat().getPlayer().getTeam().getTeamId().equals(teamId))
                .findFirst()
                .orElse(null);
    }

    private MatchLiveDTO.CurrentPlayer currentPlayer(PlayerSeasonStatBaseball stat, String teamName, String fallbackRole) {
        if (stat == null) {
            return new MatchLiveDTO.CurrentPlayer(0L, fallbackRole, teamName, "No DB player record yet");
        }

        var player = stat.getPlayerSeasonStat().getPlayer();
        String description = stat.getBattingAvg() != null
                ? "AVG " + stat.getBattingAvg() + " / H " + valueOrZero(stat.getHits()) + " / HR " + valueOrZero(stat.getHomeRuns()) + " / RBI " + valueOrZero(stat.getRbi())
                : "ERA " + valueOrDash(stat.getEra()) + " / W-L " + valueOrZero(stat.getWins()) + "-" + valueOrZero(stat.getLosses()) + " / SO " + valueOrZero(stat.getStrikeouts());
        return new MatchLiveDTO.CurrentPlayer(player.getPlayerId(), player.getName(), teamName, description);
    }

    private MatchLiveDTO.CurrentPlayer currentPlayerFromEvent(Player player, Team team) {
        String description = valueOrDefault(player.getPosition(), "포지션 정보 없음") + " · match_event 기준 현재 선수";
        return new MatchLiveDTO.CurrentPlayer(player.getPlayerId(), player.getName(), team.getName(), description);
    }

    private MatchLiveDTO fallbackLiveDto(Long matchId) {
        return new MatchLiveDTO(
                matchId,
                "baseball",
                "scheduled",
                "PRE",
                new MatchLiveDTO.TeamScore(0L, "Home team unavailable", "Home", "H", "home"),
                new MatchLiveDTO.TeamScore(0L, "Away team unavailable", "Away", "A", "away"),
                new MatchLiveDTO.Score(0, 0),
                new MatchLiveDTO.CurrentPlayer(0L, "Batter unavailable", "Home", "No match for this match_id"),
                new MatchLiveDTO.CurrentPlayer(0L, "Pitcher unavailable", "Away", "No match for this match_id"),
                new MatchLiveDTO.Count(0, 0, 0),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.InningScore("Away", List.of("-", "-", "-", "-", "-", "-", "-", "-", "-"), 0, 0, 0, 0),
                        new MatchLiveDTO.InningScore("Home", List.of("-", "-", "-", "-", "-", "-", "-", "-", "-"), 0, 0, 0, 0)
                ),
                List.of(new MatchLiveDTO.LiveEvent("result", 0, "NO MATCH", "DB match lookup failed", "Open this page from a real schedule match_id.")),
                List.of(new MatchLiveDTO.CommentPreview("system", "Fallback data is displayed because match_id was not found.", "now")),
                new MatchLiveDTO.PredictionPreview("Home", "Away", 50, 50, 0, 0, false),
                new MatchLiveDTO.ViewerState(false, false, false)
        );
    }

    private MatchLiveDTO.Count parseCount(String description) {
        Matcher matcher = COUNT_PATTERN.matcher(String.valueOf(description));
        if (!matcher.find()) {
            return new MatchLiveDTO.Count(0, 0, 0);
        }
        return new MatchLiveDTO.Count(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    private MatchLiveDTO.Runners parseRunners(String description) {
        Matcher matcher = RUNNER_PATTERN.matcher(String.valueOf(description));
        if (!matcher.find()) {
            return new MatchLiveDTO.Runners(false, false, false);
        }
        String value = matcher.group(1);
        return new MatchLiveDTO.Runners(value.contains("1"), value.contains("2"), value.contains("3"));
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return "-";
        }
        return description
                .replaceAll("\\s*\\|?\\s*B:\\d+\\s+S:\\d+\\s+O:\\d+", "")
                .replaceAll("\\s*\\|?\\s*R:[0-9,·-]+", "")
                .trim();
    }

    private String eventTypeLabel(String eventType) {
        return switch (String.valueOf(eventType).toLowerCase()) {
            case "at_bat_start" -> "타석 시작";
            case "ball" -> "볼";
            case "strike" -> "스트라이크";
            case "foul" -> "파울";
            case "single" -> "안타";
            case "double" -> "2루타";
            case "triple" -> "3루타";
            case "homerun" -> "홈런";
            case "walk" -> "볼넷";
            case "bunt" -> "번트";
            case "double_play" -> "병살타";
            case "strikeout" -> "삼진";
            case "groundout" -> "땅볼 아웃";
            case "flyout" -> "뜬공 아웃";
            case "score" -> "득점";
            case "pitcher_change" -> "투수 교체";
            case "hit" -> "인플레이";
            case "error" -> "실책";
            case "game_end" -> "경기 종료";
            default -> eventType;
        };
    }

    private int inningIndex(String eventPeriod) {
        int inning = inningNumber(eventPeriod);
        return inning >= 1 ? inning - 1 : -1;
    }

    private int inningNumber(String eventPeriod) {
        Matcher matcher = INNING_PATTERN.matcher(String.valueOf(eventPeriod));
        if (!matcher.find()) return -1;
        return Integer.parseInt(matcher.group(1));
    }

    private List<String> inningList(int[] scores) {
        List<String> result = new ArrayList<>();
        for (int score : scores) {
            result.add(String.valueOf(score));
        }
        return result;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "scheduled";
        }
        if ("LIVE".equalsIgnoreCase(status)) {
            return "in_progress";
        }
        return status;
    }

    private String shortName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "-";
        }
        return teamName.length() <= 3 ? teamName : teamName.substring(0, 3);
    }

    private String logoText(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "-";
        }
        return teamName.substring(0, 1);
    }

    private String valueOrDash(Object value) {
        return value != null ? value.toString() : "-";
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private record LiveState(
            String currentInning,
            MatchLiveDTO.Count count,
            MatchLiveDTO.Runners runners,
            List<MatchLiveDTO.LiveEvent> events
    ) {
    }

    private record EventContext(
            List<MatchEvent> rawEvents,
            List<MatchLiveDTO.LiveEvent> events,
            MatchLiveDTO.Count count,
            MatchLiveDTO.Runners runners,
            String currentInning,
            Player lastPlayer,
            Team lastTeam
    ) {
        static EventContext empty() {
            return new EventContext(
                    List.of(),
                    List.of(),
                    new MatchLiveDTO.Count(0, 0, 0),
                    new MatchLiveDTO.Runners(false, false, false),
                    "LIVE",
                    null,
                    null
            );
        }
    }
}

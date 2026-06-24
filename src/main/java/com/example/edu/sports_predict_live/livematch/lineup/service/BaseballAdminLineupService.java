package com.example.edu.sports_predict_live.livematch.lineup.service;

import com.example.edu.sports_predict_live.livematch.lineup.dto.BaseballAdminLineupDTO;
import com.example.edu.sports_predict_live.livematch.lineup.entity.MatchLineup;
import com.example.edu.sports_predict_live.livematch.lineup.repository.MatchLineupRepository;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaseballAdminLineupService {

    private static final Set<String> FIELD_STARTER_POSITIONS = Set.of("C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "DH");

    private final MatchRepository matchRepository;
    private final MatchLineupRepository matchLineupRepository;
    private final PlayerRepository playerRepository;

    public BaseballAdminLineupDTO.Editor getEditor(Long matchId) {
        Match match = findMatch(matchId);
        List<Player> homePlayers = playerRepository.findByTeam_TeamIdOrderByNameAsc(match.getHomeTeam().getTeamId());
        List<Player> awayPlayers = playerRepository.findByTeam_TeamIdOrderByNameAsc(match.getAwayTeam().getTeamId());
        List<MatchLineup> lineups = matchLineupRepository.findByMatchIdOrderByTeamIdAscStarterDescOrderNumAscMatchLineupIdAsc(matchId);
        Map<Long, MatchLineup> lineupByPlayerId = lineups.stream()
                .collect(Collectors.toMap(MatchLineup::getPlayerId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return new BaseballAdminLineupDTO.Editor(
                match.getMatchId(),
                match.getStatus(),
                match.getScheduledAt(),
                match.getVenue(),
                teamInfo(match.getHomeTeam()),
                teamInfo(match.getAwayTeam()),
                buildStatus(match, lineups),
                homePlayers.stream().map(this::playerOption).toList(),
                awayPlayers.stream().map(this::playerOption).toList(),
                homePlayers.stream().map(player -> entryFromPlayer(match, player, lineupByPlayerId.get(player.getPlayerId()))).toList(),
                awayPlayers.stream().map(player -> entryFromPlayer(match, player, lineupByPlayerId.get(player.getPlayerId()))).toList()
        );
    }

    public List<BaseballAdminLineupDTO.LineupStatus> getStatuses(List<Long> matchIds) {
        if (matchIds == null || matchIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = matchIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, List<MatchLineup>> lineupByMatchId = matchLineupRepository.findByMatchIdIn(ids).stream()
                .collect(Collectors.groupingBy(MatchLineup::getMatchId));

        return ids.stream()
                .map(matchId -> {
                    Match match = findMatch(matchId);
                    return buildStatus(match, lineupByMatchId.getOrDefault(matchId, List.of()));
                })
                .toList();
    }

    @Transactional
    public BaseballAdminLineupDTO.Editor save(Long matchId, BaseballAdminLineupDTO.SaveRequest request) {
        Match match = findMatch(matchId);
        List<BaseballAdminLineupDTO.SaveEntry> entries = request == null || request.entries() == null
                ? List.of()
                : request.entries();

        Map<Long, Player> allowedPlayerById = loadAllowedPlayers(match).stream()
                .collect(Collectors.toMap(Player::getPlayerId, Function.identity()));
        Set<Long> allowedTeamIds = Set.of(match.getHomeTeam().getTeamId(), match.getAwayTeam().getTeamId());

        List<MatchLineup> nextLineups = new ArrayList<>();
        Set<Long> includedPlayers = new HashSet<>();

        for (BaseballAdminLineupDTO.SaveEntry entry : entries) {
            if (entry == null || !Boolean.TRUE.equals(entry.included())) {
                continue;
            }
            if (entry.playerId() == null || entry.teamId() == null) {
                throw new IllegalArgumentException("라인업 저장에는 teamId와 playerId가 필요합니다.");
            }
            if (!allowedTeamIds.contains(entry.teamId())) {
                throw new IllegalArgumentException("해당 경기의 홈/원정 팀 선수가 아닙니다. teamId=" + entry.teamId());
            }
            Player player = allowedPlayerById.get(entry.playerId());
            if (player == null || player.getTeam() == null || !Objects.equals(player.getTeam().getTeamId(), entry.teamId())) {
                throw new IllegalArgumentException("해당 팀 소속 선수가 아닙니다. playerId=" + entry.playerId());
            }
            if (!includedPlayers.add(entry.playerId())) {
                throw new IllegalArgumentException("같은 선수가 라인업에 중복 포함되었습니다. playerId=" + entry.playerId());
            }

            boolean starter = Boolean.TRUE.equals(entry.starter());
            String position = normalizePosition(entry.position(), player.getPosition());
            Integer orderNum = normalizeOrderNum(entry.orderNum(), starter, position);
            nextLineups.add(MatchLineup.create(matchId, entry.teamId(), entry.playerId(), starter, orderNum, position));
        }

        validateStarterDuplicates(nextLineups);

        matchLineupRepository.deleteByMatchId(matchId);
        matchLineupRepository.saveAll(nextLineups);
        matchLineupRepository.flush();

        return getEditor(matchId);
    }

    public void assertReady(Long matchId) {
        Match match = findMatch(matchId);
        BaseballAdminLineupDTO.LineupStatus status = buildStatus(match, matchLineupRepository.findByMatchId(matchId));
        if (!status.ready()) {
            throw new IllegalStateException("선발 라인업 등록이 필요합니다. " + status.message());
        }
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
    }

    private List<Player> loadAllowedPlayers(Match match) {
        List<Player> players = new ArrayList<>();
        players.addAll(playerRepository.findByTeam_TeamIdOrderByNameAsc(match.getHomeTeam().getTeamId()));
        players.addAll(playerRepository.findByTeam_TeamIdOrderByNameAsc(match.getAwayTeam().getTeamId()));
        return players;
    }

    private BaseballAdminLineupDTO.TeamInfo teamInfo(Team team) {
        return new BaseballAdminLineupDTO.TeamInfo(team.getTeamId(), team.getName(), team.getEmblemUrl());
    }

    private BaseballAdminLineupDTO.PlayerOption playerOption(Player player) {
        return new BaseballAdminLineupDTO.PlayerOption(
                player.getPlayerId(),
                player.getTeam().getTeamId(),
                player.getTeam().getName(),
                player.getName(),
                normalizePlayerPosition(player.getPosition()),
                player.getJerseyNumber(),
                isPitcher(player.getPosition())
        );
    }

    private BaseballAdminLineupDTO.LineupEntry entryFromPlayer(Match match, Player player, MatchLineup lineup) {
        boolean included = lineup != null;
        boolean starter = lineup != null && lineup.isStarter();
        String position = lineup != null ? lineup.getPosition() : normalizePlayerPosition(player.getPosition());
        return new BaseballAdminLineupDTO.LineupEntry(
                lineup != null ? lineup.getMatchLineupId() : null,
                match.getMatchId(),
                player.getTeam().getTeamId(),
                player.getTeam().getName(),
                player.getPlayerId(),
                player.getName(),
                normalizePlayerPosition(player.getPosition()),
                player.getJerseyNumber(),
                starter,
                lineup != null ? lineup.getOrderNum() : null,
                position,
                included,
                isPitcher(position)
        );
    }

    private BaseballAdminLineupDTO.LineupStatus buildStatus(Match match, List<MatchLineup> lineups) {
        TeamLineupStatus home = teamStatus(lineups, match.getHomeTeam().getTeamId());
        TeamLineupStatus away = teamStatus(lineups, match.getAwayTeam().getTeamId());
        boolean homeReady = home.ready();
        boolean awayReady = away.ready();
        boolean ready = homeReady && awayReady;
        String message = ready
                ? "양 팀 선발 라인업 등록 완료"
                : "홈 " + home.message() + " / 원정 " + away.message();
        return new BaseballAdminLineupDTO.LineupStatus(
                match.getMatchId(),
                ready,
                homeReady,
                awayReady,
                home.fieldStarterCount,
                away.fieldStarterCount,
                home.pitcherStarterCount,
                away.pitcherStarterCount,
                message
        );
    }

    private TeamLineupStatus teamStatus(List<MatchLineup> lineups, Long teamId) {
        List<MatchLineup> starters = lineups.stream()
                .filter(MatchLineup::isStarter)
                .filter(lineup -> Objects.equals(lineup.getTeamId(), teamId))
                .toList();
        long pitcherCount = starters.stream().filter(lineup -> isPitcher(lineup.getPosition())).count();
        List<MatchLineup> fielders = starters.stream()
                .filter(lineup -> !isPitcher(lineup.getPosition()))
                .toList();
        Set<Integer> orders = fielders.stream()
                .map(MatchLineup::getOrderNum)
                .filter(order -> order != null && order >= 1 && order <= 9)
                .collect(Collectors.toSet());
        boolean ready = pitcherCount >= 1 && fielders.size() == 9 && orders.size() == 9;
        String message = ready ? "완료" : "타자 " + fielders.size() + "/9, 선발투수 " + pitcherCount + "/1";
        return new TeamLineupStatus((int) pitcherCount, fielders.size(), message);
    }

    private void validateStarterDuplicates(List<MatchLineup> lineups) {
        Map<Long, Set<Integer>> usedOrdersByTeam = new HashMap<>();
        Map<Long, Set<String>> usedPositionsByTeam = new HashMap<>();
        for (MatchLineup lineup : lineups) {
            if (!lineup.isStarter()) {
                continue;
            }
            if (isPitcher(lineup.getPosition())) {
                continue;
            }
            if (lineup.getOrderNum() == null || lineup.getOrderNum() < 1 || lineup.getOrderNum() > 9) {
                throw new IllegalArgumentException("선발 야수는 1~9번 타순이 필요합니다. playerId=" + lineup.getPlayerId());
            }
            if (!usedOrdersByTeam.computeIfAbsent(lineup.getTeamId(), ignored -> new HashSet<>()).add(lineup.getOrderNum())) {
                throw new IllegalArgumentException("같은 팀에 중복 타순이 있습니다. teamId=" + lineup.getTeamId() + ", order=" + lineup.getOrderNum());
            }
            if (!usedPositionsByTeam.computeIfAbsent(lineup.getTeamId(), ignored -> new HashSet<>()).add(lineup.getPosition())) {
                throw new IllegalArgumentException("같은 팀에 중복 선발 포지션이 있습니다. teamId=" + lineup.getTeamId() + ", position=" + lineup.getPosition());
            }
        }
    }

    private Integer normalizeOrderNum(Integer orderNum, boolean starter, String position) {
        if (!starter || isPitcher(position)) {
            return null;
        }
        return orderNum;
    }

    private String normalizePosition(String requested, String fallback) {
        String value = requested == null || requested.isBlank() ? fallback : requested;
        return normalizePlayerPosition(value);
    }

    private String normalizePlayerPosition(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("SP") || value.equals("RP") || value.equals("RHP") || value.equals("LHP")
                || value.contains("투수") || value.contains("우완") || value.contains("좌완")) {
            return "P";
        }
        if (value.contains("포수")) return "C";
        if (value.contains("1루")) return "1B";
        if (value.contains("2루")) return "2B";
        if (value.contains("3루")) return "3B";
        if (value.contains("유격")) return "SS";
        if (value.contains("좌익")) return "LF";
        if (value.contains("중견")) return "CF";
        if (value.contains("우익")) return "RF";
        if (value.contains("지명")) return "DH";
        if (FIELD_STARTER_POSITIONS.contains(value) || "P".equals(value)) return value;
        if (value.contains("외야")) return "OF";
        if (value.contains("내야")) return "IF";
        return value.isBlank() ? "-" : value;
    }

    private boolean isPitcher(String position) {
        return "P".equals(normalizePlayerPosition(position));
    }

    private record TeamLineupStatus(int pitcherStarterCount, int fieldStarterCount, String message) {
        private boolean ready() {
            return pitcherStarterCount >= 1 && fieldStarterCount == 9;
        }
    }
}

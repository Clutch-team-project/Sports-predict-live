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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoccerAdminLineupService {

    private static final Set<String> SOCCER_POSITIONS = Set.of(
            "GK", "LB", "CB", "RB", "LWB", "RWB", "DM", "CM", "AM", "LM", "RM", "LW", "RW", "FW", "ST"
    );

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
        List<Long> ids = matchIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, List<MatchLineup>> lineupByMatchId = matchLineupRepository.findByMatchIdIn(ids).stream()
                .collect(Collectors.groupingBy(MatchLineup::getMatchId));
        return ids.stream()
                .map(matchId -> buildStatus(findMatch(matchId), lineupByMatchId.getOrDefault(matchId, List.of())))
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
        Set<Long> includedPlayers = new HashSet<>();
        List<MatchLineup> nextLineups = new ArrayList<>();

        for (BaseballAdminLineupDTO.SaveEntry entry : entries) {
            if (entry == null || !Boolean.TRUE.equals(entry.included())) {
                continue;
            }
            if (entry.playerId() == null || entry.teamId() == null) {
                throw new IllegalArgumentException("lineup requires teamId and playerId");
            }
            if (!allowedTeamIds.contains(entry.teamId())) {
                throw new IllegalArgumentException("team is not in this match: " + entry.teamId());
            }
            Player player = allowedPlayerById.get(entry.playerId());
            if (player == null || player.getTeam() == null || !Objects.equals(player.getTeam().getTeamId(), entry.teamId())) {
                throw new IllegalArgumentException("player is not in this match team: " + entry.playerId());
            }
            if (!includedPlayers.add(entry.playerId())) {
                throw new IllegalArgumentException("duplicated lineup player: " + entry.playerId());
            }
            boolean starter = Boolean.TRUE.equals(entry.starter());
            String position = normalizePosition(entry.position(), player.getPosition());
            Integer orderNum = starter ? normalizeOrderNum(entry.orderNum()) : null;
            nextLineups.add(MatchLineup.create(matchId, entry.teamId(), entry.playerId(), starter, orderNum, position));
        }

        validateSoccerStarters(nextLineups);
        matchLineupRepository.deleteByMatchId(matchId);
        matchLineupRepository.saveAll(nextLineups);
        matchLineupRepository.flush();
        return getEditor(matchId);
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
                normalizePosition(player.getPosition(), null),
                player.getJerseyNumber(),
                false
        );
    }

    private BaseballAdminLineupDTO.LineupEntry entryFromPlayer(Match match, Player player, MatchLineup lineup) {
        String basePosition = normalizePosition(player.getPosition(), null);
        return new BaseballAdminLineupDTO.LineupEntry(
                lineup != null ? lineup.getMatchLineupId() : null,
                match.getMatchId(),
                player.getTeam().getTeamId(),
                player.getTeam().getName(),
                player.getPlayerId(),
                player.getName(),
                basePosition,
                player.getJerseyNumber(),
                lineup != null && lineup.isStarter(),
                lineup != null ? lineup.getOrderNum() : null,
                lineup != null ? normalizePosition(lineup.getPosition(), basePosition) : basePosition,
                lineup != null,
                false
        );
    }

    private BaseballAdminLineupDTO.LineupStatus buildStatus(Match match, List<MatchLineup> lineups) {
        int homeStarters = starterCount(lineups, match.getHomeTeam().getTeamId());
        int awayStarters = starterCount(lineups, match.getAwayTeam().getTeamId());
        boolean homeReady = homeStarters == 11;
        boolean awayReady = awayStarters == 11;
        boolean ready = homeReady && awayReady;
        String message = ready ? "soccer lineup ready" : "home " + homeStarters + "/11 / away " + awayStarters + "/11";
        return new BaseballAdminLineupDTO.LineupStatus(
                match.getMatchId(),
                ready,
                homeReady,
                awayReady,
                homeStarters,
                awayStarters,
                0,
                0,
                message
        );
    }

    private int starterCount(List<MatchLineup> lineups, Long teamId) {
        return (int) lineups.stream()
                .filter(MatchLineup::isStarter)
                .filter(lineup -> Objects.equals(lineup.getTeamId(), teamId))
                .count();
    }

    private void validateSoccerStarters(List<MatchLineup> lineups) {
        Map<Long, Long> starterCounts = lineups.stream()
                .filter(MatchLineup::isStarter)
                .collect(Collectors.groupingBy(MatchLineup::getTeamId, Collectors.counting()));
        starterCounts.forEach((teamId, count) -> {
            if (count > 11) {
                throw new IllegalArgumentException("soccer starters cannot exceed 11. teamId=" + teamId);
            }
        });
    }

    private Integer normalizeOrderNum(Integer orderNum) {
        return orderNum != null && orderNum >= 1 && orderNum <= 11 ? orderNum : null;
    }

    private String normalizePosition(String requested, String fallback) {
        String value = requested == null || requested.isBlank() ? fallback : requested;
        value = value == null ? "" : value.trim().toUpperCase();
        if (value.equals("FWD")) value = "FW";
        if (value.equals("ATT")) value = "ST";
        if (value.equals("MID")) value = "CM";
        if (value.equals("DEF")) value = "CB";
        if (SOCCER_POSITIONS.contains(value)) return value;
        return value.isBlank() ? "CM" : value;
    }
}

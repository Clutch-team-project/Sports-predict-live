package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchLineupDTO;
import com.example.edu.sports_predict_live.livematch.entity.MatchLineup;
import com.example.edu.sports_predict_live.livematch.repository.MatchLineupRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchLineupService {

    private final MatchLineupRepository matchLineupRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public List<MatchLineupDTO> getLineups(Long matchId) {
        List<MatchLineup> lineups = matchLineupRepository.findByMatchIdOrderByTeamIdAscStarterDescOrderNumAscMatchLineupIdAsc(matchId);

        Set<Long> playerIds = lineups.stream().map(MatchLineup::getPlayerId).collect(Collectors.toSet());
        Set<Long> teamIds = lineups.stream().map(MatchLineup::getTeamId).collect(Collectors.toSet());

        Map<Long, Player> playerById = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getPlayerId, player -> player));

        Map<Long, Team> teamById = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getTeamId, team -> team));

        return lineups.stream()
                .map(lineup -> MatchLineupDTO.from(
                        lineup,
                        teamById.get(lineup.getTeamId()),
                        playerById.get(lineup.getPlayerId())
                ))
                .toList();
    }
}
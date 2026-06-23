package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchPlayerStatDTO;
import com.example.edu.sports_predict_live.livematch.entity.MatchPlayerStat;
import com.example.edu.sports_predict_live.livematch.repository.MatchPlayerStatRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchPlayerStatService {

    private final MatchPlayerStatRepository matchPlayerStatRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    public List<MatchPlayerStatDTO> getPlayerStats(Long matchId) {
        List<MatchPlayerStat> stats = matchPlayerStatRepository.findByMatchIdOrderByTeamIdAscPlayerIdAscStatKeyAsc(matchId);

        Set<Long> playerIds = stats.stream().map(MatchPlayerStat::getPlayerId).collect(Collectors.toSet());
        Set<Long> teamIds = stats.stream().map(MatchPlayerStat::getTeamId).collect(Collectors.toSet());

        Map<Long, Player> playerById = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getPlayerId, player -> player));

        Map<Long, Team> teamById = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getTeamId, team -> team));

        Map<Long, List<MatchPlayerStat>> statsByPlayer = stats.stream()
                .collect(Collectors.groupingBy(
                        MatchPlayerStat::getPlayerId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return statsByPlayer.values().stream()
                .map(playerStats -> {
                    MatchPlayerStat first = playerStats.get(0);
                    return MatchPlayerStatDTO.from(
                            playerById.get(first.getPlayerId()),
                            teamById.get(first.getTeamId()),
                            playerStats
                    );
                })
                .toList();
    }
}

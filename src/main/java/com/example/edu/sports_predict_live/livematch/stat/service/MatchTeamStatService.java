package com.example.edu.sports_predict_live.livematch.stat.service;

import com.example.edu.sports_predict_live.livematch.stat.dto.MatchTeamStatDTO;
import com.example.edu.sports_predict_live.livematch.stat.entity.MatchTeamStat;
import com.example.edu.sports_predict_live.livematch.stat.repository.MatchTeamStatRepository;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.team.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchTeamStatService {

    private final MatchTeamStatRepository matchTeamStatRepository;
    private final MatchRepository matchRepository;

    public Map<String, List<MatchTeamStatDTO>> getTeamStats(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));

        List<MatchTeamStat> stats = matchTeamStatRepository.findByMatchIdOrderByTeamIdAscStatKeyAsc(matchId);

        Map<String, List<MatchTeamStatDTO>> result = new LinkedHashMap<>();
        result.put("home", toDtos(stats, match.getHomeTeam()));
        result.put("away", toDtos(stats, match.getAwayTeam()));
        return result;
    }

    private List<MatchTeamStatDTO> toDtos(List<MatchTeamStat> stats, Team team) {
        return stats.stream()
                .filter(stat -> stat.getTeamId().equals(team.getTeamId()))
                .map(stat -> MatchTeamStatDTO.from(stat, team))
                .toList();
    }
}

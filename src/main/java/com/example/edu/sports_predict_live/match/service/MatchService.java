package com.example.edu.sports_predict_live.match.service;

import com.example.edu.sports_predict_live.livematch.event.service.MatchEventService;
import com.example.edu.sports_predict_live.livematch.lineup.service.MatchLineupService;
import com.example.edu.sports_predict_live.livematch.stat.service.MatchPlayerStatService;
import com.example.edu.sports_predict_live.livematch.stat.service.MatchTeamStatService;
import com.example.edu.sports_predict_live.match.dto.MatchDTO;
import com.example.edu.sports_predict_live.match.dto.MatchDetailDTO;
import com.example.edu.sports_predict_live.match.dto.MatchListDTO;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.dto.response.PlayerResponseDTO;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchEventService matchEventService;
    private final MatchLineupService matchLineupService;
    private final MatchTeamStatService matchTeamStatService;
    private final MatchPlayerStatService matchPlayerStatService;
    private final PlayerRepository playerRepository;

    public List<MatchListDTO> getGames(Long sportId, LocalDate date, String status) {
        return matchRepository.findGames(sportId, date, status).stream()
                .map(MatchListDTO::from)
                .toList();
    }

    public MatchDetailDTO getGame(Long matchId) {
        Match match = matchRepository.findByIdWithTeams(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));

        return new MatchDetailDTO(
                MatchDTO.from(match),
                matchEventService.getMatchEvents(matchId),
                matchLineupService.getLineups(matchId),
                matchTeamStatService.getTeamStats(matchId),
                matchPlayerStatService.getPlayerStats(matchId),
                playerRepository.findByTeam_TeamIdOrderByNameAsc(match.getHomeTeam().getTeamId()).stream()
                        .map(PlayerResponseDTO::new)
                        .toList(),
                playerRepository.findByTeam_TeamIdOrderByNameAsc(match.getAwayTeam().getTeamId()).stream()
                        .map(PlayerResponseDTO::new)
                        .toList()
        );
    }
}

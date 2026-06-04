package com.example.edu.sports_predict_live.team.service;

import com.example.edu.sports_predict_live.team.dto.response.StandingsResponseDTO;
import com.example.edu.sports_predict_live.team.repository.TeamSeasonStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingsService {

    private final TeamSeasonStatRepository teamSeasonStatRepository;

    private static final String CURRENT_SEASON = "2026";

    public List<StandingsResponseDTO> getStandings(String sport) {
        return teamSeasonStatRepository
                .findBySportCodeAndSeason(sport, CURRENT_SEASON)
                .stream()
                .map(StandingsResponseDTO::new)
                .toList();
    }
}

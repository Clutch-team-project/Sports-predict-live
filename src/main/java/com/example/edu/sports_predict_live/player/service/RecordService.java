package com.example.edu.sports_predict_live.player.service;

import com.example.edu.sports_predict_live.player.dto.response.HitterRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.LolPlayerRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.PitcherRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.SoccerPlayerRecordResponseDTO;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatLolRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatSoccerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final PlayerSeasonStatBaseballRepository baseballRepository;
    private final PlayerSeasonStatLolRepository      lolRepository;
    private final PlayerSeasonStatSoccerRepository   soccerRepository;

    private static final String CURRENT_SEASON = "2026";

    public List<HitterRecordResponseDTO> getHitters(String sport) {
        return baseballRepository
                .findHittersBySportAndSeason(sport, CURRENT_SEASON)
                .stream()
                .map(HitterRecordResponseDTO::new)
                .toList();
    }

    public List<PitcherRecordResponseDTO> getPitchers(String sport) {
        return baseballRepository
                .findPitchersBySportAndSeason(sport, CURRENT_SEASON)
                .stream()
                .map(PitcherRecordResponseDTO::new)
                .toList();
    }

    public List<LolPlayerRecordResponseDTO> getLolPlayers() {
        return lolRepository
                .findBySeasonOrderByKdaDesc(CURRENT_SEASON)
                .stream()
                .map(LolPlayerRecordResponseDTO::new)
                .toList();
    }

    public List<SoccerPlayerRecordResponseDTO> getSoccerPlayers() {
        return soccerRepository
                .findBySeasonOrderByGoalsDesc(CURRENT_SEASON)
                .stream()
                .map(SoccerPlayerRecordResponseDTO::new)
                .toList();
    }
}
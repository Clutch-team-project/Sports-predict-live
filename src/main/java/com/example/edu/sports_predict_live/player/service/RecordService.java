package com.example.edu.sports_predict_live.player.service;

import com.example.edu.sports_predict_live.player.dto.response.HitterRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.PitcherRecordResponseDTO;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordService {

    private final PlayerSeasonStatBaseballRepository baseballRepository;

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
}

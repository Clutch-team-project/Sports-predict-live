package com.example.edu.sports_predict_live.player.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.player.dto.response.PlayerDetailResponseDTO;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStat;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatLol;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatSoccer;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatLolRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatSoccerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 선수 상세 페이지 데이터 — 기본 정보 + 현재 시즌 종목별 기록(야구/축구/LOL)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerDetailService {

    private static final String CURRENT_SEASON = "2026";

    private final PlayerRepository playerRepository;
    private final PlayerSeasonStatRepository playerSeasonStatRepository;
    private final PlayerSeasonStatBaseballRepository baseballRepository;
    private final PlayerSeasonStatSoccerRepository soccerRepository;
    private final PlayerSeasonStatLolRepository lolRepository;

    public PlayerDetailResponseDTO getPlayerDetail(Long playerId) {
        Player player = playerRepository.findByIdWithTeam(playerId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAYER_NOT_FOUND));

        PlayerSeasonStat pss = playerSeasonStatRepository
                .findByPlayerIdAndSeason(playerId, CURRENT_SEASON)
                .orElse(null);

        PlayerSeasonStatBaseball baseball = null;
        PlayerSeasonStatSoccer soccer = null;
        PlayerSeasonStatLol lol = null;

        if (pss != null) {
            Long statId = pss.getPlayerSeasonStatId();
            String sportCode = player.getTeam().getSport().getCode();
            switch (sportCode) {
                case "baseball" -> baseball = baseballRepository.findById(statId).orElse(null);
                case "soccer"   -> soccer   = soccerRepository.findById(statId).orElse(null);
                case "lol"      -> lol      = lolRepository.findById(statId).orElse(null);
            }
        }

        return new PlayerDetailResponseDTO(player, pss, baseball, soccer, lol);
    }
}

package com.example.edu.sports_predict_live.aiprediction.service;

import com.example.edu.sports_predict_live.aiprediction.entity.MatchEntity;
import com.example.edu.sports_predict_live.aiprediction.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchEntity saveMatch(

            String homeTeam,
            String awayTeam,
            Integer homeScore,
            Integer awayScore
    ) {

        MatchEntity match = new MatchEntity();

        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);

        match.setMatchDate(LocalDateTime.now());

        return matchRepository.save(match);
    }

    public List<MatchEntity> getAllMatches() {

        return matchRepository.findAll();
    }
} //실제 db 연결 시 /match/list, /match/detail 같은 API 바로 붙일 수 있게 기본 구조 미리 만드는 단계

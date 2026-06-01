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

            Long sportId,
            Long homeTeamId,
            Long awayTeamId,
            Integer homeScore,
            Integer awayScore,
            String status,
            String venue
    ) {

        MatchEntity match = new MatchEntity();

        match.setSportId(sportId);
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);

        match.setStatus(status);
        match.setVenue(venue);

        match.setScheduledAt(LocalDateTime.now());

        return matchRepository.save(match);
    }

    public List<MatchEntity> getAllMatches() {

        return matchRepository.findAll();
    }

    public MatchEntity getMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("경기 없음"));
    }

    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }

    public MatchEntity updateMatch(Long id, MatchEntity match) {

        MatchEntity existing = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("경기 없음"));

        existing.setStatus(match.getStatus());
        existing.setHomeScore(match.getHomeScore());
        existing.setAwayScore(match.getAwayScore());
        existing.setVenue(match.getVenue());

        return matchRepository.save(existing);
    }
} //실제 db 연결 시 /match/list, /match/detail 같은 API 바로 붙일 수 있게 기본 구조 미리 만드는 단계

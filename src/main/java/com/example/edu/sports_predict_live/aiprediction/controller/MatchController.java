package com.example.edu.sports_predict_live.aiprediction.controller;

import com.example.edu.sports_predict_live.aiprediction.entity.MatchEntity;
import com.example.edu.sports_predict_live.aiprediction.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/save")
    public MatchEntity saveMatch(

            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam Integer homeScore,
            @RequestParam Integer awayScore
    ) {

        return matchService.saveMatch(
                homeTeam,
                awayTeam,
                homeScore,
                awayScore
        );
    }

    @GetMapping("/all")
    public List<MatchEntity> getAllMatches() {

        return matchService.getAllMatches();
    }
} //전체 조회 GET /match/list 단건 조회 GET /match/detail?matchId=1
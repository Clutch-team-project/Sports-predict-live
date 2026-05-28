package com.example.edu.sports_predict_live.controller;

import com.example.edu.sports_predict_live.entity.MatchEntity;
import com.example.edu.sports_predict_live.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    // 전체 경기 조회
    @GetMapping("/list")
    public List<MatchEntity> getAllMatches() {

        return matchService.getAllMatches();
    }

    // 경기 단건 조회
    @GetMapping("/detail")
    public MatchEntity getMatch(
            @RequestParam Long matchId
    ) {

        return matchService.getMatch(matchId);
    }
} //전체 조회 GET /match/list 단건 조회 GET /match/detail?matchId=1
package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.MatchTeamStatDTO;
import com.example.edu.sports_predict_live.livematch.service.MatchTeamStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class MatchTeamStatController {

    private final MatchTeamStatService matchTeamStatService;

    @GetMapping("/{matchId}/team-stats")
    public ResponseEntity<Map<String, List<MatchTeamStatDTO>>> getTeamStats(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchTeamStatService.getTeamStats(matchId));
    }
}

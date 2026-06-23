package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.MatchPlayerStatDTO;
import com.example.edu.sports_predict_live.livematch.service.MatchPlayerStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class MatchPlayerStatController {

    private final MatchPlayerStatService matchPlayerStatService;

    @GetMapping("/{matchId}/player-stats")
    public ResponseEntity<List<MatchPlayerStatDTO>> getPlayerStats(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchPlayerStatService.getPlayerStats(matchId));
    }
}

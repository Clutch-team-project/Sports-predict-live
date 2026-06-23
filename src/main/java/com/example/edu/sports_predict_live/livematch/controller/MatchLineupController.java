package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.MatchLineupDTO;
import com.example.edu.sports_predict_live.livematch.service.MatchLineupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class MatchLineupController {

    private final MatchLineupService matchLineupService;

    @GetMapping("/{matchId}/lineup")
    public ResponseEntity<List<MatchLineupDTO>> getLineup(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchLineupService.getLineups(matchId));
    }
}
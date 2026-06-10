package com.example.edu.sports_predict_live.team.controller;

import com.example.edu.sports_predict_live.team.dto.response.StandingsResponseDTO;
import com.example.edu.sports_predict_live.team.service.StandingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
public class StandingsController {

    private final StandingsService standingsService;

    // GET /api/standings/{sport} — baseball/soccer는 DB, lol은 lolesports API 실시간 조회
    @GetMapping("/{sport}")
    public ResponseEntity<List<StandingsResponseDTO>> getStandings(
            @PathVariable String sport) {
        return ResponseEntity.ok(standingsService.getStandings(sport));
    }
}

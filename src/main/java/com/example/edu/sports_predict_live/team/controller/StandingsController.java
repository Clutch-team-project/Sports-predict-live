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

    // GET /api/standings/baseball
    // GET /api/standings/soccer
    // GET /api/standings/lol
    @GetMapping("/{sport}")
    public ResponseEntity<List<StandingsResponseDTO>> getStandings(
            @PathVariable String sport) {
        return ResponseEntity.ok(standingsService.getStandings(sport));
    }
}

package com.example.edu.sports_predict_live.match.controller;

import com.example.edu.sports_predict_live.match.dto.MatchDetailDTO;
import com.example.edu.sports_predict_live.match.dto.MatchListDTO;
import com.example.edu.sports_predict_live.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<List<MatchListDTO>> getGames(
            @RequestParam(name = "sport_id", required = false) Long sportId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(matchService.getGames(sportId, date, status));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchDetailDTO> getGame(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getGame(matchId));
    }
}

package com.example.edu.sports_predict_live.livematch.soccer.controller;

import com.example.edu.sports_predict_live.livematch.soccer.dto.SoccerLiveDTO;
import com.example.edu.sports_predict_live.livematch.soccer.service.SoccerLiveStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class SoccerLiveController {

    private final SoccerLiveStateService soccerLiveStateService;

    @GetMapping("/{matchId}/soccer-live")
    public ResponseEntity<SoccerLiveDTO> getSoccerLive(@PathVariable Long matchId) {
        return ResponseEntity.ok(soccerLiveStateService.getSoccerLive(matchId));
    }
}

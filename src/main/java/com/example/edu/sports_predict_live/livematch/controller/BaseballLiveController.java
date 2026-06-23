package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.BaseballLiveDTO;
import com.example.edu.sports_predict_live.livematch.service.BaseballLiveStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class BaseballLiveController {

    private final BaseballLiveStateService baseballLiveStateService;

    @GetMapping("/{matchId}/baseball-live")
    public ResponseEntity<BaseballLiveDTO> getBaseballLive(@PathVariable Long matchId) {
        return ResponseEntity.ok(baseballLiveStateService.getBaseballLive(matchId));
    }
}

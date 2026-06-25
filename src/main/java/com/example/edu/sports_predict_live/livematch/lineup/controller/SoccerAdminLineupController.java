package com.example.edu.sports_predict_live.livematch.lineup.controller;

import com.example.edu.sports_predict_live.livematch.lineup.dto.BaseballAdminLineupDTO;
import com.example.edu.sports_predict_live.livematch.lineup.service.SoccerAdminLineupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/soccer")
@RequiredArgsConstructor
public class SoccerAdminLineupController {

    private final SoccerAdminLineupService soccerAdminLineupService;

    @GetMapping("/games/lineup-status")
    public ResponseEntity<List<BaseballAdminLineupDTO.LineupStatus>> lineupStatuses(
            @RequestParam List<Long> matchIds
    ) {
        return ResponseEntity.ok(soccerAdminLineupService.getStatuses(matchIds));
    }

    @GetMapping("/games/{matchId}/lineup-editor")
    public ResponseEntity<BaseballAdminLineupDTO.Editor> lineupEditor(@PathVariable Long matchId) {
        return ResponseEntity.ok(soccerAdminLineupService.getEditor(matchId));
    }

    @PutMapping("/games/{matchId}/lineup")
    public ResponseEntity<BaseballAdminLineupDTO.Editor> saveLineup(
            @PathVariable Long matchId,
            @RequestBody BaseballAdminLineupDTO.SaveRequest request
    ) {
        return ResponseEntity.ok(soccerAdminLineupService.save(matchId, request));
    }
}

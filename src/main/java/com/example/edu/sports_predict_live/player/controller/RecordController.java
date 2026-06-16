package com.example.edu.sports_predict_live.player.controller;

import com.example.edu.sports_predict_live.player.dto.response.HitterRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.LolPlayerRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.PitcherRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.SoccerPlayerRecordResponseDTO;
import com.example.edu.sports_predict_live.player.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 선수 기록 API — 야구(타자/투수), 축구, LOL 시즌 기록 랭킹
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/{sport}/hitters")
    public ResponseEntity<List<HitterRecordResponseDTO>> getHitters(
            @PathVariable String sport) {
        return ResponseEntity.ok(recordService.getHitters(sport));
    }

    @GetMapping("/{sport}/pitchers")
    public ResponseEntity<List<PitcherRecordResponseDTO>> getPitchers(
            @PathVariable String sport) {
        return ResponseEntity.ok(recordService.getPitchers(sport));
    }

    @GetMapping("/lol/players")
    public ResponseEntity<List<LolPlayerRecordResponseDTO>> getLolPlayers() {
        return ResponseEntity.ok(recordService.getLolPlayers());
    }

    @GetMapping("/soccer/players")
    public ResponseEntity<List<SoccerPlayerRecordResponseDTO>> getSoccerPlayers() {
        return ResponseEntity.ok(recordService.getSoccerPlayers());
    }
}
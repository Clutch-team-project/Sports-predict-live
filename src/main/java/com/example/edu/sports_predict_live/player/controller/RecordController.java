package com.example.edu.sports_predict_live.player.controller;

import com.example.edu.sports_predict_live.player.dto.response.HitterRecordResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.PitcherRecordResponseDTO;
import com.example.edu.sports_predict_live.player.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    // GET /api/records/baseball/hitters  — 타자 기록
    @GetMapping("/{sport}/hitters")
    public ResponseEntity<List<HitterRecordResponseDTO>> getHitters(
            @PathVariable String sport) {
        return ResponseEntity.ok(recordService.getHitters(sport));
    }

    // GET /api/records/baseball/pitchers  — 투수 기록
    @GetMapping("/{sport}/pitchers")
    public ResponseEntity<List<PitcherRecordResponseDTO>> getPitchers(
            @PathVariable String sport) {
        return ResponseEntity.ok(recordService.getPitchers(sport));
    }
}

package com.example.edu.sports_predict_live.match.controller;

import com.example.edu.sports_predict_live.match.dto.response.MatchResponseDTO;
import com.example.edu.sports_predict_live.match.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // GET /api/schedule/{sport}?date=20260604 — date 미지정 시 오늘
    @GetMapping("/{sport}")
    public ResponseEntity<List<MatchResponseDTO>> getSchedule(
            @PathVariable String sport,
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(scheduleService.getSchedule(sport, date));
    }

    @GetMapping("/lol")
    public ResponseEntity<List<Map<String, Object>>> getLolSchedule(
            @RequestParam(required = false) String date) {
        return ResponseEntity.ok(scheduleService.getLolSchedule(date));
    }
}

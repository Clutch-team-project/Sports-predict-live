package com.example.edu.sports_predict_live.livematch.event.controller;

import com.example.edu.sports_predict_live.livematch.event.dto.MatchEventDTO;
import com.example.edu.sports_predict_live.livematch.event.service.MatchEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MatchEventController {

    private final MatchEventService matchEventService;

    @GetMapping("/api/games/{matchId}/events")
    public ResponseEntity<List<MatchEventDTO>> getMatchEvents(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchEventService.getMatchEvents(matchId));
    }

    // TODO: 관리자 권한은 기존 SecurityConfig의 /api/admin/** hasRole("ADMIN") 정책을 따른다.
    @PostMapping("/api/admin/games/{matchId}/events")
    public ResponseEntity<MatchEventDTO> createAdminEvent(
            @PathVariable Long matchId,
            @RequestBody MatchEventDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(matchEventService.saveAdminEvent(matchId, request));
    }
}
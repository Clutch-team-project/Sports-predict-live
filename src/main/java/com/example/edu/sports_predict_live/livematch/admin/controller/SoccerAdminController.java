package com.example.edu.sports_predict_live.livematch.admin.controller;

import com.example.edu.sports_predict_live.livematch.admin.dto.SoccerAdminCommandDTO;
import com.example.edu.sports_predict_live.livematch.admin.service.SoccerAdminCommandService;
import com.example.edu.sports_predict_live.livematch.soccer.dto.SoccerLiveDTO;
import com.example.edu.sports_predict_live.livematch.soccer.service.SoccerLiveStateService;
import com.example.edu.sports_predict_live.match.dto.response.MatchResponseDTO;
import com.example.edu.sports_predict_live.match.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/soccer")
@RequiredArgsConstructor
public class SoccerAdminController {

    private final ScheduleService scheduleService;
    private final SoccerLiveStateService soccerLiveStateService;
    private final SoccerAdminCommandService soccerAdminCommandService;

    @GetMapping("/games")
    public ResponseEntity<List<MatchResponseDTO>> games(@RequestParam(required = false) String date) {
        if (date == null || date.isBlank()) {
            return ResponseEntity.ok(scheduleService.getScheduleAll("soccer"));
        }
        return ResponseEntity.ok(scheduleService.getSchedule("soccer", date.replace("-", "")));
    }

    @GetMapping("/games/{matchId}/state")
    public ResponseEntity<SoccerLiveDTO> state(@PathVariable Long matchId) {
        return ResponseEntity.ok(soccerLiveStateService.getSoccerLive(matchId));
    }

    @PostMapping("/games/{matchId}/command")
    public ResponseEntity<SoccerLiveDTO> command(@PathVariable Long matchId, @RequestBody SoccerAdminCommandDTO command) {
        return ResponseEntity.ok(soccerAdminCommandService.apply(matchId, command));
    }

    @DeleteMapping("/games/{matchId}/events")
    public ResponseEntity<SoccerLiveDTO> clearEvents(@PathVariable Long matchId) {
        return ResponseEntity.ok(soccerAdminCommandService.clearEvents(matchId));
    }
}

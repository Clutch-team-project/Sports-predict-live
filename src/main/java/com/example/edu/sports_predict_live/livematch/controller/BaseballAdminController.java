package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.BaseballAdminCommandDTO;
import com.example.edu.sports_predict_live.livematch.dto.BaseballLiveDTO;
import com.example.edu.sports_predict_live.livematch.service.BaseballAdminCommandService;
import com.example.edu.sports_predict_live.livematch.service.BaseballLiveStateService;
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
@RequestMapping("/api/admin/baseball")
@RequiredArgsConstructor
public class BaseballAdminController {

    private final ScheduleService scheduleService;
    private final BaseballLiveStateService baseballLiveStateService;
    private final BaseballAdminCommandService baseballAdminCommandService;

    @GetMapping("/games")
    public ResponseEntity<List<MatchResponseDTO>> games(@RequestParam(required = false) String date) {
        if (date == null || date.isBlank()) {
            return ResponseEntity.ok(scheduleService.getScheduleAll("baseball"));
        }
        return ResponseEntity.ok(scheduleService.getSchedule("baseball", date.replace("-", "")));
    }

    @GetMapping("/games/{matchId}/state")
    public ResponseEntity<BaseballLiveDTO> state(@PathVariable Long matchId) {
        return ResponseEntity.ok(baseballLiveStateService.getBaseballLive(matchId));
    }

    @PostMapping("/games/{matchId}/command")
    public ResponseEntity<BaseballLiveDTO> command(
            @PathVariable Long matchId,
            @RequestBody BaseballAdminCommandDTO command
    ) {
        return ResponseEntity.ok(baseballAdminCommandService.apply(matchId, command));
    }

    @DeleteMapping("/games/{matchId}/events")
    public ResponseEntity<BaseballLiveDTO> clearEvents(@PathVariable Long matchId) {
        return ResponseEntity.ok(baseballAdminCommandService.clearEvents(matchId));
    }
}

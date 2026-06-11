package com.example.edu.sports_predict_live.team.controller;

import com.example.edu.sports_predict_live.team.dto.response.TeamDetailResponseDTO;
import com.example.edu.sports_predict_live.team.dto.response.TeamSimpleResponseDTO;
import com.example.edu.sports_predict_live.team.service.TeamDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 팀 정보 API — 팀 목록(이름→ID 매핑용), 팀 상세(기본 정보+시즌 성적+선수단+경기)
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamDetailService teamDetailService;

    // GET /api/teams?sport=lol
    @GetMapping
    public ResponseEntity<List<TeamSimpleResponseDTO>> getTeams(
            @RequestParam String sport) {
        return ResponseEntity.ok(teamDetailService.getTeamsBySport(sport));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailResponseDTO> getTeamDetail(
            @PathVariable Long teamId) {
        return ResponseEntity.ok(teamDetailService.getTeamDetail(teamId));
    }
}

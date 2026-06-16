package com.example.edu.sports_predict_live.player.controller;

import com.example.edu.sports_predict_live.player.dto.response.PlayerDetailResponseDTO;
import com.example.edu.sports_predict_live.player.dto.response.PlayerResponseDTO;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.player.service.PlayerDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 선수 API — 종목별 선수 목록, 선수 상세(기본 정보+시즌 기록)
@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final PlayerDetailService playerDetailService;

    // GET /api/players?sport=lol
    @GetMapping
    public ResponseEntity<List<PlayerResponseDTO>> getPlayers(
            @RequestParam String sport) {
        List<PlayerResponseDTO> result = playerRepository
                .findBySportCode(sport)
                .stream()
                .map(PlayerResponseDTO::new)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerDetailResponseDTO> getPlayerDetail(
            @PathVariable Long playerId) {
        return ResponseEntity.ok(playerDetailService.getPlayerDetail(playerId));
    }
}

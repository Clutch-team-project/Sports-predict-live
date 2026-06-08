package com.example.edu.sports_predict_live.player.controller;

import com.example.edu.sports_predict_live.player.dto.response.PlayerResponseDTO;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;

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
}

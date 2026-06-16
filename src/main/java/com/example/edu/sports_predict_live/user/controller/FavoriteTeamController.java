package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.response.FavoriteTeamResponseDTO;
import com.example.edu.sports_predict_live.user.service.FavoriteTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 관심 팀 API — 등록/해제/목록 조회 (순위표 ★ 버튼, 내 정보 페이지에서 사용)
@RestController
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
public class FavoriteTeamController {

    private final FavoriteTeamService favoriteTeamService;

    @GetMapping
    public ResponseEntity<List<FavoriteTeamResponseDTO>> getFavorites(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(favoriteTeamService.getFavorites(userId));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getFavoriteIds(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(favoriteTeamService.getFavoriteTeamIds(userId));
    }

    @PostMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> addFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        favoriteTeamService.addFavorite(userId, teamId);
        return ResponseEntity.ok(Map.of("message", "관심 팀으로 등록되었습니다."));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> removeFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        favoriteTeamService.removeFavorite(userId, teamId);
        return ResponseEntity.ok(Map.of("message", "관심 팀에서 삭제되었습니다."));
    }
}

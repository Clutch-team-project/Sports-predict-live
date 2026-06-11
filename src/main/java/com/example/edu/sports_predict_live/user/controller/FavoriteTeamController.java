package com.example.edu.sports_predict_live.user.controller;

import com.example.edu.sports_predict_live.user.dto.response.FavoriteTeamResponseDTO;
import com.example.edu.sports_predict_live.user.service.FavoriteTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me/favorites")
@RequiredArgsConstructor
public class FavoriteTeamController {

    private final FavoriteTeamService favoriteTeamService;

    // 관심 팀 목록 조회
    @GetMapping
    public ResponseEntity<List<FavoriteTeamResponseDTO>> getFavorites(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(favoriteTeamService.getFavorites(userId));
    }

    // 관심 팀 ID 목록 조회 (프론트 ★ 상태 초기화용)
    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getFavoriteIds(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(favoriteTeamService.getFavoriteTeamIds(userId));
    }

    // 관심 팀 추가
    @PostMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> addFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        favoriteTeamService.addFavorite(userId, teamId);
        return ResponseEntity.ok(Map.of("message", "관심 팀으로 등록되었습니다."));
    }

    // 관심 팀 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Map<String, String>> removeFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long teamId) {
        favoriteTeamService.removeFavorite(userId, teamId);
        return ResponseEntity.ok(Map.of("message", "관심 팀에서 삭제되었습니다."));
    }
}

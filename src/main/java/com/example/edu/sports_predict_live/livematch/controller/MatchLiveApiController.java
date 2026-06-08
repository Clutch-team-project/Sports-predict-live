package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.MatchLiveDTO;
import com.example.edu.sports_predict_live.livematch.service.MatchLiveDummyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class MatchLiveApiController {

    private final MatchLiveDummyService matchLiveDummyService;

    /*
     * 더미 중계 데이터 API.
     * Postman에서 GET /api/games/1/baseball-live 를 호출하면 이 메서드가 실행된다.
     *
     * 흐름:
     * 1. URL의 {matchId} 값을 받는다.
     * 2. MatchLiveDummyService에 더미 데이터 생성을 맡긴다.
     * 3. MatchLiveDTO를 JSON으로 반환한다.
     *
     * 아직 HTML이 이 API를 fetch하지 않으므로 화면에는 반영되지 않는다.
     */
    @GetMapping("/{matchId}/baseball-live")
    public ResponseEntity<MatchLiveDTO> getBaseballLive(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchLiveDummyService.getBaseballLive(matchId));
    }
}

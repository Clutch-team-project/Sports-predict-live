package com.example.edu.sports_predict_live.livematch.controller;

import com.example.edu.sports_predict_live.livematch.dto.MatchEventDTO;
import com.example.edu.sports_predict_live.livematch.service.MatchEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class MatchEventController {

    private final MatchEventService matchEventService;

    /*
     * match_event 조회 API.
     * Postman에서 GET /api/games/1/events 를 호출하면 이 메서드가 실행된다.
     *
     * 흐름:
     * 1. URL의 {gameId} 값을 받는다.
     * 2. Service에 "이 경기의 이벤트 목록을 조회해 달라"고 넘긴다.
     * 3. Service가 DB 조회 결과를 DTO로 바꾸면 JSON 배열로 반환한다.
     *
     * DB에 해당 gameId의 match_event 행이 없으면 빈 배열 []가 반환된다.
     */
    @GetMapping("/{gameId}/events")
    public ResponseEntity<List<MatchEventDTO>> getMatchEvents(@PathVariable Long gameId) {
        return ResponseEntity.ok(matchEventService.getMatchEvents(gameId));
    }
}

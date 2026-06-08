package com.example.edu.sports_predict_live.livematch.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MatchLivePageController {

    /*
     * 화면 URL 매핑.
     * 사용자가 /games/1/baseball/live 로 들어오면 이 Controller가 먼저 받는다.
     * 여기서는 DB나 API를 조회하지 않고, templates/baseball-live-match.html 화면만 열어준다.
     *
     * 이 파일은 버려지는 파일이 아니다.
     * "중계 화면을 보여주는 URL" 담당이고, 아래 API Controller들은 "JSON 데이터를 주는 URL" 담당이다.
     */
    @GetMapping("/games/{matchId}/baseball/live")
    public String baseballLive(@PathVariable Long matchId) {
        // matchId는 URL에서 받은 경기 ID다. 지금 단계에서는 화면만 띄우므로 아직 사용하지 않는다.
        return "baseball-live-match";
    }
}

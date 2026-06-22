package com.example.edu.sports_predict_live.baseball;

import com.example.edu.sports_predict_live.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/baseball")
@RequiredArgsConstructor
public class BaseballViewController {

    private final BoardService boardService;

    // 야구 홈 (메인 홈 구조 기반, 상단 AI 배너 제외 / 야구 경기·뉴스만 노출)
    @GetMapping
    public String home(Model model) {
        model.addAttribute("findTop5ViewCountToday", boardService.findTop5ViewCountToday());
        return "baseball/index";
    }

    // 야구 순위/기록 페이지 (팀순위, 팀기록, 타자기록, 투수기록 탭 통합)
    @GetMapping("/standings")
    public String standings() {
        return "baseball/standings";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "baseball/schedule";
    }
}

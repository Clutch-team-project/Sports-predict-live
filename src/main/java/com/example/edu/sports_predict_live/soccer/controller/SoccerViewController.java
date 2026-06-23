package com.example.edu.sports_predict_live.soccer.controller;

import com.example.edu.sports_predict_live.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/soccer")
@RequiredArgsConstructor
public class SoccerViewController {

    private final BoardService boardService;

    // 축구 홈 (메인 홈 구조 기반, 상단 AI 배너 제외 / 축구 경기·뉴스만 노출)
    @GetMapping
    public String home(Model model) {
        model.addAttribute("findTop5ViewCountToday", boardService.findTop5ViewCountToday("soccer"));
        return "soccer/index";
    }

    @GetMapping("/standings")
    public String standings() {
        return "soccer/standings";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "soccer/schedule";
    }
}
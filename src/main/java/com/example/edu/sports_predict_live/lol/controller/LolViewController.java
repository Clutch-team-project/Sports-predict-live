package com.example.edu.sports_predict_live.lol.controller;

import com.example.edu.sports_predict_live.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lol")
@RequiredArgsConstructor
public class LolViewController {

    private final BoardService boardService;

    // LOL 홈 (메인 홈 구조 기반, 상단 AI 배너 제외 / LOL 경기·뉴스만 노출)
    @GetMapping
    public String home(Model model) {
        model.addAttribute("findTop5ViewCountToday", boardService.findTop5ViewCountToday());
        return "lol/index";
    }

    @GetMapping("/standings")
    public String standings() {
        return "lol/standings";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "lol/schedule";
    }
}

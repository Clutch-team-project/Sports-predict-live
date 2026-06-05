package com.example.edu.sports_predict_live.baseball;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/baseball")
public class BaseballViewController {

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

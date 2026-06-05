package com.example.edu.sports_predict_live.lol.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lol")
public class LolViewController {

    @GetMapping("/standings")
    public String standings() {
        return "lol/standings";
    }

    @GetMapping("/schedule")
    public String schedule() {
        return "lol/schedule";
    }
}

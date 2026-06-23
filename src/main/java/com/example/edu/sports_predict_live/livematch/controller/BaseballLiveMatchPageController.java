package com.example.edu.sports_predict_live.livematch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseballLiveMatchPageController {

    @GetMapping("/baseball/live-match")
    public String baseballLiveMatch() {
        return "baseball-live-match";
    }
}

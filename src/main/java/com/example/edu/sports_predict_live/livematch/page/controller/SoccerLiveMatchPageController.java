package com.example.edu.sports_predict_live.livematch.page.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SoccerLiveMatchPageController {

    @GetMapping({"/soccer/live-match", "/soccer-live-match", "/games/{matchId}/soccer/live"})
    public String soccerLiveMatch(@PathVariable(required = false) Long matchId) {
        return "soccer-live-match";
    }
}

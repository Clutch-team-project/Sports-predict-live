package com.example.edu.sports_predict_live.soccer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/soccer")
public class SoccerViewController {

    @GetMapping("/standings")
    public String standings() {
        return "soccer/standings";
    }
}
package com.example.edu.sports_predict_live.news.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NewsPageController {

    @GetMapping("/news")
    public String newsPage() {
        return "news";
    }
}
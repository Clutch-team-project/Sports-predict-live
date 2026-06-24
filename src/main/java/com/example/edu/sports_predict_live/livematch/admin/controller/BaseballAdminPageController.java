package com.example.edu.sports_predict_live.livematch.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseballAdminPageController {

    @GetMapping("/admin/control")
    public String adminControl() {
        return "admin-control";
    }
}

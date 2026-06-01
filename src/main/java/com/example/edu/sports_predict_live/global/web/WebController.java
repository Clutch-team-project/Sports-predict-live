package com.example.edu.sports_predict_live.global.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/notification-agreement")
    public String notificationAgreement() {
        return "notification-agreement";
    }

    @GetMapping("/signup-success")
    public String signupSuccess() {
        return "signup-success";
    }

    @GetMapping("/login-success")
    public String loginSuccess() {
        return "login-success";
    }

    @GetMapping("/user-info")
    public String userInfo() {
        return "user-info";
    }

    @GetMapping("/find-id")
    public String findId() {
        return "find-id";
    }

    @GetMapping("/find-password")
    public String findPassword() {
        return "find-password";
    }

    @GetMapping("/change-password")
    public String changePassword() {
        return "change-password";
    }

    @GetMapping("/prediction-history")
    public String predictionHistory() {
        return "prediction-history";
    }
}
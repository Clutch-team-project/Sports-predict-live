package com.example.edu.sports_predict_live.livematch.admin.demo.controller;

import com.example.edu.sports_predict_live.livematch.admin.demo.dto.DemoAutoStatusDTO;
import com.example.edu.sports_predict_live.livematch.admin.demo.service.DemoAutoEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/demo/games/{matchId}/auto")
public class DemoAutoEventController {

    private final DemoAutoEventService demoAutoEventService;

    @GetMapping("/status")
    public DemoAutoStatusDTO status(@PathVariable Long matchId) {
        return demoAutoEventService.status(matchId);
    }

    @PostMapping("/start")
    public DemoAutoStatusDTO start(@PathVariable Long matchId) {
        return demoAutoEventService.start(matchId);
    }

    @PostMapping("/stop")
    public DemoAutoStatusDTO stop(@PathVariable Long matchId) {
        return demoAutoEventService.stop(matchId);
    }
}

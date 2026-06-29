package com.example.edu.sports_predict_live.livematch.admin.demo.dto;

public record DemoAutoStatusDTO(
        Long matchId,
        boolean running,
        long totalSeeds,
        long publishedSeeds,
        long remainingSeeds,
        long elapsedSeconds,
        Integer nextDelaySeconds
) {
}

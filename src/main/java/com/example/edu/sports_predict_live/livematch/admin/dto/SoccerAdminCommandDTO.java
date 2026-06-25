package com.example.edu.sports_predict_live.livematch.admin.dto;

public record SoccerAdminCommandDTO(
        SoccerAdminAction action,
        Long teamId,
        Long playerId,
        Long inPlayerId,
        Long outPlayerId,
        Integer minute,
        String period,
        String description
) {
    public String normalizedPeriod() {
        if (period == null || period.isBlank()) {
            return minute != null && minute > 45 ? "second_half" : "first_half";
        }
        return period.trim();
    }

    public int normalizedMinute() {
        return minute == null ? 0 : Math.max(0, minute);
    }
}

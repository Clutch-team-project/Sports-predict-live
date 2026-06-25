package com.example.edu.sports_predict_live.livematch.admin.dto;

public record SoccerAdminCommandDTO(
        SoccerAdminAction action,
        Long teamId,
        Long playerId,
        Long inPlayerId,
        Long outPlayerId,
        Integer minute,
        Integer addedMinute,
        String period,
        String description
) {
    public String normalizedPeriod() {
        if (period == null || period.isBlank()) {
            return normalizedMinute() > 45 ? "second_half" : "first_half";
        }
        return period.trim();
    }

    public int normalizedMinute() {
        int base = minute == null ? 0 : Math.max(0, minute);
        int added = addedMinute == null ? 0 : Math.max(0, addedMinute);
        return base + added;
    }
}

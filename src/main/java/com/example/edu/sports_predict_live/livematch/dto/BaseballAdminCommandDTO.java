package com.example.edu.sports_predict_live.livematch.dto;

public record BaseballAdminCommandDTO(
        BaseballAdminAction action,
        Long playerId,
        Long runnerId,
        Long teamId,
        String base,
        String description,
        Integer pitchSpeed,
        String pitchType
) {
    public String normalizedPitchDescription() {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        if (pitchSpeed == null && (pitchType == null || pitchType.isBlank())) {
            return null;
        }
        String speed = pitchSpeed == null ? "" : pitchSpeed + "km/h";
        String type = pitchType == null ? "" : pitchType.trim();
        return (speed + " " + type).trim();
    }
}

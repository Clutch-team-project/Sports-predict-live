package com.example.edu.sports_predict_live.livematch.comment.dto;

import jakarta.validation.constraints.Size;

public record MatchCommentReportDTO(
        @Size(max = 200)
        String reason
) {
}

package com.example.edu.sports_predict_live.livematch.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatchCommentCreateDTO(
        @NotBlank
        @Size(max = 300)
        String content,
        Long supportTeamId
) {
}

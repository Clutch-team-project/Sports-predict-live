package com.example.edu.sports_predict_live.livematch.comment.dto;

import com.example.edu.sports_predict_live.livematch.comment.entity.MatchComment;

import java.time.LocalDateTime;

public record MatchCommentDTO(
        Long commentId,
        Long matchId,
        Long userId,
        String nickname,
        Long supportTeamId,
        String supportTeamName,
        String supportTeamEmblemUrl,
        String content,
        LocalDateTime createdAt
) {
    public static MatchCommentDTO from(MatchComment comment) {
        String nickname = comment.getUser().getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = comment.getUser().getName();
        }
        return new MatchCommentDTO(
                comment.getCommentId(),
                comment.getMatch().getMatchId(),
                comment.getUser().getUserId(),
                nickname,
                comment.getSupportTeam() == null ? null : comment.getSupportTeam().getTeamId(),
                comment.getSupportTeam() == null ? null : comment.getSupportTeam().getName(),
                comment.getSupportTeam() == null ? null : comment.getSupportTeam().getEmblemUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}

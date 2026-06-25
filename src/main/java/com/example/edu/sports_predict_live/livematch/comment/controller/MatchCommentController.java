package com.example.edu.sports_predict_live.livematch.comment.controller;

import com.example.edu.sports_predict_live.livematch.comment.dto.MatchCommentCreateDTO;
import com.example.edu.sports_predict_live.livematch.comment.dto.MatchCommentDTO;
import com.example.edu.sports_predict_live.livematch.comment.dto.MatchCommentReportDTO;
import com.example.edu.sports_predict_live.livematch.comment.service.MatchCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games/{matchId}/comments")
public class MatchCommentController {

    private final MatchCommentService matchCommentService;

    @GetMapping
    public ResponseEntity<List<MatchCommentDTO>> getComments(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchCommentService.getComments(matchId));
    }

    @PostMapping
    public ResponseEntity<MatchCommentDTO> createComment(
            @PathVariable Long matchId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MatchCommentCreateDTO request
    ) {
        return ResponseEntity.ok(matchCommentService.createComment(matchId, userId, request));
    }

    @PostMapping("/{commentId}/reports")
    public ResponseEntity<Void> reportComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) MatchCommentReportDTO request
    ) {
        matchCommentService.reportComment(commentId, userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long matchId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId
    ) {
        matchCommentService.deleteComment(matchId, commentId, userId);
        return ResponseEntity.ok().build();
    }
}

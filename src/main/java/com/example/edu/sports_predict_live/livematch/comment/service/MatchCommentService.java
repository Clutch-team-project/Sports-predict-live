package com.example.edu.sports_predict_live.livematch.comment.service;

import com.example.edu.sports_predict_live.livematch.comment.dto.MatchCommentCreateDTO;
import com.example.edu.sports_predict_live.livematch.comment.dto.MatchCommentDTO;
import com.example.edu.sports_predict_live.livematch.comment.dto.MatchCommentReportDTO;
import com.example.edu.sports_predict_live.livematch.comment.entity.MatchComment;
import com.example.edu.sports_predict_live.livematch.comment.entity.MatchCommentReport;
import com.example.edu.sports_predict_live.livematch.comment.repository.MatchCommentReportRepository;
import com.example.edu.sports_predict_live.livematch.comment.repository.MatchCommentRepository;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MatchCommentService {

    private static final int DEFAULT_LIMIT = 50;

    private final MatchCommentRepository matchCommentRepository;
    private final MatchCommentReportRepository matchCommentReportRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<MatchCommentDTO> getComments(Long matchId) {
        return matchCommentRepository.findByMatch_MatchIdAndDeletedFalseOrderByCreatedAtDescCommentIdDesc(
                        matchId,
                        PageRequest.of(0, DEFAULT_LIMIT)
                ).stream()
                .map(MatchCommentDTO::from)
                .toList();
    }

    @Transactional
    public MatchCommentDTO createComment(Long matchId, Long userId, MatchCommentCreateDTO request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("match not found: " + matchId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));

        String content = request.content().trim();
        Team supportTeam = resolveSupportTeam(match, request.supportTeamId());
        MatchComment saved = matchCommentRepository.save(new MatchComment(match, user, supportTeam, content));
        MatchCommentDTO dto = MatchCommentDTO.from(saved);
        messagingTemplate.convertAndSend("/topic/games/" + matchId + "/comments", dto);
        return dto;
    }

    @Transactional
    public void reportComment(Long commentId, Long userId, MatchCommentReportDTO request) {
        if (matchCommentReportRepository.existsByComment_CommentIdAndReporter_UserId(commentId, userId)) {
            return;
        }

        MatchComment comment = matchCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("comment not found: " + commentId));
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + userId));
        String reason = request == null ? null : request.reason();

        matchCommentReportRepository.save(new MatchCommentReport(comment, reporter, reason));
    }

    @Transactional
    public void deleteComment(Long matchId, Long commentId, Long userId) {
        MatchComment comment = matchCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("comment not found: " + commentId));
        if (!comment.getMatch().getMatchId().equals(matchId)) {
            throw new IllegalArgumentException("comment does not belong to match: " + matchId);
        }
        if (!comment.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("comment owner mismatch");
        }
        comment.delete();
        messagingTemplate.convertAndSend(
                "/topic/games/" + matchId + "/comments",
                Map.of("type", "delete", "commentId", commentId)
        );
    }

    private Team resolveSupportTeam(Match match, Long supportTeamId) {
        if (supportTeamId == null) {
            return null;
        }
        Long homeTeamId = match.getHomeTeam() == null ? null : match.getHomeTeam().getTeamId();
        Long awayTeamId = match.getAwayTeam() == null ? null : match.getAwayTeam().getTeamId();
        if (!supportTeamId.equals(homeTeamId) && !supportTeamId.equals(awayTeamId)) {
            return null;
        }
        return teamRepository.findById(supportTeamId).orElse(null);
    }
}

package com.example.edu.sports_predict_live.livematch.comment.repository;

import com.example.edu.sports_predict_live.livematch.comment.entity.MatchCommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchCommentReportRepository extends JpaRepository<MatchCommentReport, Long> {

    boolean existsByComment_CommentIdAndReporter_UserId(Long commentId, Long reporterId);
}

package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.BoardReplyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardReplyReportRepository extends JpaRepository<BoardReplyReport, Long> {
    Optional<BoardReplyReport> findByBoardReply_ReplyIdAndUserId(Long replyId, Long userId);

    List<BoardReplyReport> findByBoardReply_ReplyIdInAndUserId(List<Long> replyIds, Long userId);
}
package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.BoardReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardReplyLikeRepository extends JpaRepository<BoardReplyLike, Long> {
    Optional<BoardReplyLike> findByBoardReply_ReplyIdAndUserId(Long replyId, Long userId);

    List<BoardReplyLike> findByBoardReply_ReplyIdInAndUserId(List<Long> replyIds, Long userId);
}

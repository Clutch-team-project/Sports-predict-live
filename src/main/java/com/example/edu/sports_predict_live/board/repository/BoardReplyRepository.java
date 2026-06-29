package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.BoardReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardReplyRepository extends JpaRepository<BoardReply, Long> {
    Page<BoardReply> findByBoard_BoardIdAndParentIsNullAndDeletedAtIsNull(Long boardId, Pageable pageable);
    List<BoardReply> findByParent_ReplyIdInAndDeletedAtIsNull(List<Long> parentIds);
    int countByBoard_BoardIdAndDeletedAtIsNull(Long boardId);
}

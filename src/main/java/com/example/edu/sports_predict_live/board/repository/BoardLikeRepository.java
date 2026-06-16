package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.entity.BoardLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {
    Optional<BoardLike> findByBoard_BoardIdAndUserId(Long boardId, Long userId);

}

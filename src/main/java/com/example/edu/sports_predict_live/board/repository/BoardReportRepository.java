package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.BoardReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardReportRepository extends JpaRepository<BoardReport, Long> {
    Optional<BoardReport> findByBoard_BoardIdAndUserId(Long boardId, Long userId);
}

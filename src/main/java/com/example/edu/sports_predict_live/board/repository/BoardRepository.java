package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.repository.search.BoardSearch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {

    @Query(value = "select now()", nativeQuery = true)
    String getTime();

    @EntityGraph(attributePaths = {"imageSet"})
    @Query("select b from Board b where b.boardId = :boardId and b.deletedAt is null")
    Optional<Board> findByIdWithImages(@Param("boardId") Long boardId);

    List<Board> findByIsNoticeTrueOrderByBoardIdDesc();

    @Query
    List<Board> findTop5ByCreatedAtBetweenAndDeletedAtIsNullAndIsBlindedFalseOrderByViewCountDesc(LocalDateTime start, LocalDateTime end);}

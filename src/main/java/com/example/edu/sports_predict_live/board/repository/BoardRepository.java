package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.search.BoardSearch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {

    @Query(value = "select now()", nativeQuery = true)
    String getTime();

    @EntityGraph(attributePaths = {"imageSet"})
    @Query("select b from Board b where b.boardId = :boardId")
    Optional<Board> findByIdWithImages(@Param("boardId") Long boardId);
}

package com.example.edu.sports_predict_live.board.repository;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.repository.search.BoardSearch;
import org.springframework.data.domain.Pageable;
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

    // 각 게시판(boardType)별 오늘 자 실시간 인기글 5개 가져옴
    @Query("select b from Board b " +
            "where b.createdAt between :start and :end " +
            "and b.deletedAt is null " +
            "and b.isBlinded = false " +
            "and b.isNotice = false " +
            "and (:boardType is null or b.boardType = :boardType) " +
            "order by b.viewCount desc, b.boardId desc")
    List<Board> findTop5PopularByBoardType(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("boardType") String boardType,
            Pageable pageable
    );
}
package com.example.edu.sports_predict_live.board.search;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListReplyCountDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearch {

    // 기본 검색
    Page<Board> searchAll(String[] types, String keyword, Pageable pageable);
    // 댓글 개수 포함 검색
    Page<BoardListReplyCountDTO> searchWithReplyCount(String[] types, String keyword, Pageable pageable);
    // 댓글 개수 + 이미지 포함된 검색
    Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, Pageable pageable);
}

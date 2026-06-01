package com.example.edu.sports_predict_live.board.search;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearch {

    // 댓글 개수, 이미지 포함 검색
    Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, Pageable pageable);
}

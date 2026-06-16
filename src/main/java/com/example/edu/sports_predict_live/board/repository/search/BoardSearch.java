package com.example.edu.sports_predict_live.board.repository.search;

<<<<<<< HEAD

=======
>>>>>>> 97ad1653951578eb2122423fe33e13fe5a28b0a5
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearch {
    // 댓글 개수, 이미지, 카테고리, 글정렬용 view, like 등 받는 sort 포함검색
<<<<<<< HEAD
    Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, String category, String sort, Pageable pageable);
}
=======
    Page<BoardListAllDTO> searchWithAll(String[] types, String keyword, String sort, String category, Pageable pageable);
}//
>>>>>>> 97ad1653951578eb2122423fe33e13fe5a28b0a5

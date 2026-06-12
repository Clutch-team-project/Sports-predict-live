package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.dto.BoardReplyDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;

public interface BoardReplyService {
    Long register(BoardReplyDTO boardReplyDTO);
    BoardReplyDTO read(Long replyId);
    void modify(BoardReplyDTO boardReplyDTO);
    void remove(Long replyId);

    PageResponseDTO<BoardReplyDTO> getListOfBoard(Long boardId, PageRequestDTO pageRequestDTO);
}

package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.dto.BoardReplyDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;

public interface BoardReplyService {
    BoardReplyDTO read(Long replyId);
    Long register(BoardReplyDTO boardReplyDTO);
    void modifyReply(BoardReplyDTO boardReplyDTO, Long currentUserId);
    void removeReply(Long replyId, Long currentUserId, String currentUserRole);
    void toggleLikeReply(Long replyId, Long currentUserId);
    void reportReply(Long replyId, Long currentUserId);
    void toggleBlindReply(Long replyId, String currentUserRole);

    PageResponseDTO<BoardReplyDTO> getListOfBoard(Long boardId, PageRequestDTO pageRequestDTO, Long currentUserId);
}

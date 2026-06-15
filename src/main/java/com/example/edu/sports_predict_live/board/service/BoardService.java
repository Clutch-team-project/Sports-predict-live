package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.stream.Collectors;

public interface BoardService {
    Long register(BoardDTO boardDTO, String currentUserRole);
    BoardDTO readOne(Long boardId);
    BoardDTO getBoardOnly(Long boardId);
    void modify(BoardDTO boardDTO);
    void remove(Long boardID, Long currentUserId, String currentUserRole);
    void toggleLike(Long boardId, Long userId);
    boolean checkIsLiked(Long boardId, Long userId);
    void report(Long boardId, Long userId);
    boolean checkIsReported(Long boardId, Long userId);
    void toggleBlind(Long boardId);

    PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO);

    default Board dtoToEntity(BoardDTO boardDTO) {
        Board board = Board.builder()
                .boardId(boardDTO.getBoardId())
                .userId(boardDTO.getUserId())
                .category(boardDTO.getCategory())
                .title(boardDTO.getTitle())
                .content(boardDTO.getContent())
                .isNotice(boardDTO.isNotice())
                .build();
        if(boardDTO.getFileNames() != null) {
            boardDTO.getFileNames().forEach(fileName -> {
                String[] arr = fileName.split("_", 2);
                if (arr.length == 2) {
                    board.addImage(arr[0], arr[1]);
                } else {
                    board.addImage(java.util.UUID.randomUUID().toString(), fileName);
                }
            });
        }
        return board;
    }

    default BoardDTO entityToDTO(Board board) {
        List<String> fileNames = board.getImageSet().stream()
                .sorted() // ord 기준 정렬
                .map(boardImage -> boardImage.getUuid() + "_" + boardImage.getFileName())
                .collect(Collectors.toList());

        return BoardDTO.builder()
                .boardId(board.getBoardId())
                .userId(board.getUserId())
                .category(board.getCategory())
                .title(board.getTitle())
                .content(board.getContent())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .isNotice(board.isNotice())
                .isBlinded(board.isBlinded())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .fileNames(fileNames)
                .build();
    }


}

package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public interface BoardService {
    Long register(BoardDTO boardDTO); // 글 등록
    BoardDTO readOne(Long boardId); // 글 상세 조회
    void modify(BoardDTO boardDTO); // 글 수정
    void remove(Long boardID); // 글 삭제

    PageResponseDTO<BoardListAllDTO> list(PageRequestDTO pageRequestDTO);

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

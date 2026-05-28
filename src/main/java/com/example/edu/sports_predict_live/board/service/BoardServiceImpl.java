package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class BoardServiceImpl implements BoardService{
    private final ModelMapper modelMapper;
    private final BoardRepository boardRepository;

    @Override
    public Long register(BoardDTO boardDTO) {
        // 기본 게시글 엔티티 생성
        Board board = dtoToEntity(boardDTO);
        Long boardId = boardRepository.save(board).getBoardId();
        return boardId;
    }

    @Override
    public BoardDTO readOne(Long boardId){
        Optional<Board> result = boardRepository.findByIdWithImages(boardId);
        Board board = result.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. id=" + boardId));
        BoardDTO boardDTO = entityToDTO(board);
        return boardDTO;
    }

    @Override
    public void modify(BoardDTO boardDTO) {
        Optional<Board> result = boardRepository.findByIdWithImages(boardDTO.getBoardId());
        Board board = result.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. id=" + boardDTO.getBoardId()));
        board.change(boardDTO.getTitle(), boardDTO.getContent(), boardDTO.getCategory(), boardDTO.isNotice());
        board.clearImage(); // 기존 이미지 파일 제거
        if(boardDTO.getFileNames() != null) {
            for(String fileName : boardDTO.getFileNames()) {
                String[] arr = fileName.split("_", 2);
                if(arr.length == 2) {
                    board.addImage(arr[0], arr[1]);
                } else {
                    board.addImage(java.util.UUID.randomUUID().toString(), fileName);
                }

            }
        }
        boardRepository.save(board);
    }

    @Override
    public void remove(Long boardId) {
        boardRepository.deleteById(boardId);
    }
}

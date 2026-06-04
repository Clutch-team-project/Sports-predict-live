package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.repository.BoardRepository;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserRepository userRepository;

    // 게시글 생성
    @Override
    public Long register(BoardDTO boardDTO) {
        Board board = dtoToEntity(boardDTO);
        Long boardId = boardRepository.save(board).getBoardId();
        return boardId;
    }
    // 게시글 상세 확인(조회수 증가)
    @Override
    public BoardDTO readOne(Long boardId){
        Optional<Board> result = boardRepository.findByIdWithImages(boardId);
        Board board = result.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. id=" + boardId));
        board.changeViewCount(board.getViewCount() + 1);
        BoardDTO boardDTO = entityToDTO(board);

        Optional<User> userOptional = userRepository.findById(board.getUserId());
        if(userOptional.isPresent()) {
            User user = userOptional.get();
            boardDTO.setLoginId(user.getLoginId());
            boardDTO.setNickname(user.getNickname());
        }

        return boardDTO;
    }
    // 게시글 수정용 게시글 상세확인(조회수 증가 X)
    @Override
    public BoardDTO getBoardOnly(Long boardId) {
        Optional<Board> result = boardRepository.findByIdWithImages(boardId);
        Board board = result.orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. id=" + boardId));
        BoardDTO boardDTO = entityToDTO(board);

        Optional<User> userOptional = userRepository.findById(board.getUserId());
        if(userOptional.isPresent()) {
            User user = userOptional.get();
            boardDTO.setLoginId(user.getLoginId());
            boardDTO.setNickname(user.getNickname());
        }

        return boardDTO;
    }
    // 게시글 수정
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
    // 게시글 삭제
    @Override
    public void remove(Long boardId) {
        Optional<Board> result = boardRepository.findById(boardId);
        Board board = result.orElseThrow();
        board.softDelete();

        boardRepository.save(board);
    }

    @Override
    public PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO) {
        String[] types = pageRequestDTO.getTypes();
        String keyword = pageRequestDTO.getKeyword();
        String category = pageRequestDTO.getCategory();
        String sort = pageRequestDTO.getSort();
        Pageable pageable = pageRequestDTO.getPageable("boardId");
        Page<BoardListAllDTO> result = boardRepository.searchWithAll(types, keyword, category, sort, pageable);
        return PageResponseDTO.<BoardListAllDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(result.getContent())
                .total((int)result.getTotalElements())
                .build();
    }
}

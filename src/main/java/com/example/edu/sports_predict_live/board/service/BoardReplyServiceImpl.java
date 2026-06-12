package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.dto.BoardReplyDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.entity.BoardReply;
import com.example.edu.sports_predict_live.board.repository.BoardReplyRepository;
import com.example.edu.sports_predict_live.board.repository.BoardRepository;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardReplyServiceImpl implements BoardReplyService {
    private final BoardReplyRepository replyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Long register(BoardReplyDTO boardReplyDTO) {
        Board board = boardRepository.findById(boardReplyDTO.getBoardId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        User user = userRepository.findById(boardReplyDTO.getUserId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        String nickName = (user.getNickname() != null && !user.getNickname().trim().isEmpty() ? user.getNickname() : "익명");

        BoardReply reply = BoardReply.builder()
                .board(board)
                .replyText(boardReplyDTO.getReplyText())
                .userId(boardReplyDTO.getUserId())
                .nickname(nickName)
                .build();

        return replyRepository.save(reply).getReplyId();
    }

    @Override
    public BoardReplyDTO read(Long replyId) {
        BoardReply reply = replyRepository.findById(replyId).orElseThrow();
        return modelMapper.map(reply, BoardReplyDTO.class);
    }

    @Override
    public void modify(BoardReplyDTO boardReplyDTO) {
        BoardReply reply = replyRepository.findById(boardReplyDTO.getReplyId()).orElseThrow();
        reply.changeText(boardReplyDTO.getReplyText());
        replyRepository.save(reply);
    }

    @Override
    public void remove(Long replyId) {
        replyRepository.deleteById(replyId);
    }

    @Override
    public PageResponseDTO<BoardReplyDTO> getListOfBoard(Long boardId, PageRequestDTO pageRequestDTO) {
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() <= 0 ? 0 : pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by("replyId").ascending()
        );

        Page<BoardReply> result = replyRepository.findByBoard_BoardId(boardId, pageable);

        List<BoardReplyDTO> dtoList = result.getContent().stream()
                .map(reply -> BoardReplyDTO.builder()
                        .replyId(reply.getReplyId())
                        .boardId(boardId)
                        .replyText(reply.getReplyText())
                        .userId(reply.getUserId())
                        .nickname(reply.getNickname())
                        .createdAt(reply.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResponseDTO.<BoardReplyDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total((int)result.getTotalElements())
                .build();
    }
}

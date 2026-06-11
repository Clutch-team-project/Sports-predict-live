package com.example.edu.sports_predict_live.board.controller;

import com.example.edu.sports_predict_live.board.dto.BoardReplyDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.service.BoardReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class BoardReplyController {
    private final BoardReplyService replyService;

    // 댓글 등록
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> register(@RequestBody BoardReplyDTO replyDTO) {
        Long replyId = replyService.register(replyDTO);
        return ResponseEntity.ok(Map.of("replyId", replyId));
    }

    // 특정 게시글의 댓글 목록 조회
    @GetMapping(value = "/list/{boardId}")
    public ResponseEntity<PageResponseDTO<BoardReplyDTO>> getList(
            @PathVariable("boardId") Long boardId,
            PageRequestDTO pageRequestDTO) {

        PageResponseDTO<BoardReplyDTO> responseDTO = replyService.getListOfBoard(boardId, pageRequestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    // 댓글 삭제
    @DeleteMapping("/{replyId}")
    public ResponseEntity<Map<String, String>> remove(@PathVariable("replyId") Long replyId) {
        replyService.remove(replyId);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 댓글 수정
    @PutMapping(value = "/{replyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> modify(
            @PathVariable("replyId") Long replyId,
            @RequestBody BoardReplyDTO replyDTO) {

        replyDTO.setReplyId(replyId);
        replyService.modify(replyDTO);
        return ResponseEntity.ok(Map.of("result", "success"));
    }
}

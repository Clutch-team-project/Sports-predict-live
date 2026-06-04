package com.example.edu.sports_predict_live.board.controller;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
    private final BoardService boardService;

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/board/list";
    }

    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model) {
        log.info("게시판 목록 조회 요청 : " + pageRequestDTO);
        PageResponseDTO<BoardListAllDTO> responseDTO = boardService.listWithAll(pageRequestDTO);
        model.addAttribute("responseDTO", responseDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        log.info("프론트에 넘기는 데이터 : " + responseDTO);
    }

    @GetMapping("/read")
    public void read(Long boardId, PageRequestDTO pageRequestDTO, Model model) {
        BoardDTO boardDTO = boardService.readOne(boardId);
        model.addAttribute("dto", boardDTO);
    }

    @GetMapping("/modify")
    public void modify(Long boardId, PageRequestDTO pageRequestDTO, Model model) {
        BoardDTO boardDTO = boardService.getBoardOnly(boardId);
        model.addAttribute("dto", boardDTO);
    }


}

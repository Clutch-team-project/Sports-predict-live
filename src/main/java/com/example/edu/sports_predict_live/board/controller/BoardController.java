package com.example.edu.sports_predict_live.board.controller;

import com.example.edu.sports_predict_live.board.domain.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/register")
    public String registerGET() {
        return "board/register";
    }

    @PostMapping("/register")
    public String registerPOST(BoardDTO boardDTO, Authentication authentication, RedirectAttributes redirectAttributes) {
        // 현재 로그인 된 유저의 아이디or 이메일 가져오기
        if (authentication != null && authentication.isAuthenticated()) {
            String loginId = authentication.getName(); // 토큰에 담아둔 subject (보통 로그인 ID)
        }
        // DB에 Insert 후 생성된 글 번호(boardId) 반환
        Long boardId = boardService.register(boardDTO);
        // 작성 성공 시 목록으로 리다이렉트 되면서 'N번 글이 등록되었습니다' 전송
        redirectAttributes.addFlashAttribute("result", boardId);
        // /board/list 화면으로 이동
        return "redirect:/board/list";
    }



}

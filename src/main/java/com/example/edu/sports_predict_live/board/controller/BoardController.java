package com.example.edu.sports_predict_live.board.controller;

import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.service.BoardService;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {
    private final BoardService boardService;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    private Long getCurrentUserId(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인 후 이용 가능합니다.");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            User user = userRepository.findByLoginIdAndDeletedAtIsNull(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
            return user.getUserId();
        }
    }

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
    public void read(Long boardId, PageRequestDTO pageRequestDTO, Model model, HttpServletRequest request) {
        BoardDTO boardDTO = boardService.readOne(boardId);
        model.addAttribute("dto", boardDTO);

        Long currentUserId = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    try {
                        if (jwtProvider.validateToken(token) && "access".equals(jwtProvider.getTokenType(token))) {
                            currentUserId = jwtProvider.getUserId(token);
                        }
                    } catch (Exception e) {
                    }
                    break;
                }
            }
        }
        // 좋아요 눌렀는지 여부 확인
        boolean isLiked = false;
        if(currentUserId != null) {
            isLiked = boardService.checkIsLiked(boardId, currentUserId);
        }

        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("isLiked", isLiked);
    }

    @GetMapping("/register")
    public String registerGET() {
        return "board/register";
    }

    @PostMapping("/register")
    public String registerPOST(BoardDTO boardDTO, Authentication authentication, RedirectAttributes redirectAttributes) {
        Long currentUserId = getCurrentUserId(authentication);
        boardDTO.setUserId(currentUserId);

        // DB에 Insert 후 생성된 글 번호(boardId) 반환
        Long boardId = boardService.register(boardDTO);

        redirectAttributes.addFlashAttribute("result", boardId);
        return "redirect:/board/list";
    }

    @GetMapping("/modify")
    public void modify(Long boardId, PageRequestDTO pageRequestDTO, Model model) {
        BoardDTO boardDTO = boardService.getBoardOnly(boardId);
        model.addAttribute("dto", boardDTO);
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResponseEntity<String> modifyPOST(@Valid BoardDTO boardDTO, BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 올바르지 않습니다.");
        }

        try {
            // 현재 로그인한 유저 ID 추출
            Long currentUserId = getCurrentUserId(authentication);
            BoardDTO existingBoard = boardService.getBoardOnly(boardDTO.getBoardId());

            // DB에 userId가 null인 경우의 에러 방지 처리
            if (existingBoard.getUserId() == null || !existingBoard.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 권한이 없습니다.");
            }

            // 정상적으로 권한이 확인되면 수정 진행
            boardDTO.setUserId(currentUserId);
            boardService.modify(boardDTO);
            return ResponseEntity.ok("success");

        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생: ", e);
            // 에러가 났을 때 정확한 이유를 프론트엔드에 전달
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류: " + e.getMessage());
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<String> removePOST(@RequestParam("boardId") Long boardId, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            BoardDTO existingBoard = boardService.getBoardOnly(boardId);

            // 작성자 비교
            if (!existingBoard.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다.");
            }

            boardService.remove(boardId);
            return ResponseEntity.ok("success");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/like")
    @ResponseBody
    public ResponseEntity<String> like(@RequestParam("boardId") Long boardId, Authentication authentication){
        try {
            // 현재 로그인한 사용자 id 가져오기
            Long currentUserId = getCurrentUserId(authentication);
            // 좋아요 수 증가 or 감소
            boardService.toggleLike(boardId, currentUserId);

            return ResponseEntity.ok("");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다.");
        }
    }

//    @PostMapping("/report")
//    @ResponseBody
//    public ResponseEntity<String> report(@RequestParam("boardId") Long boardId, Authentication authentication) {
//        try {
//            Long currentUserId = getCurrentUserId(authentication);
//
//            return ResponseEntity.ok("");
//        }
//    }



}

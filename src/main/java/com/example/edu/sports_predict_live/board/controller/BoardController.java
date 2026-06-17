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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // 현재 로그인 한 유저의 ID 가져오기
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
    // 현재 로그인한 유저의 ROLE 가져오기
    private String getCurrentUserRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "ROLE_USER";
        }
        return authentication.getAuthorities().iterator().next().getAuthority();
    }

    // board -> board/list 로 이동
    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/board/list";
    }

    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Authentication authentication, Model model) {
        log.info("게시판 목록 조회 요청 : ", pageRequestDTO);
        PageResponseDTO<BoardListAllDTO> responseDTO = boardService.listWithAll(pageRequestDTO);
        String currentUserRole = getCurrentUserRole(authentication);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("responseDTO", responseDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        log.info("프론트에 넘기는 데이터 : ", responseDTO);
    }

    @GetMapping("/read")
    public void read(Long boardId, PageRequestDTO pageRequestDTO, Model model, HttpServletRequest request, Authentication authentication) {
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

        boolean isLiked = false;
        boolean isReported = false;

        if(currentUserId != null) {
            isLiked = boardService.checkIsLiked(boardId, currentUserId);
            isReported = boardService.checkIsReported(boardId, currentUserId);
        }

        String currentUserRole = getCurrentUserRole(authentication);

        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("isReported", isReported);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
    }

    @GetMapping("/register")
    public String registerGET(Authentication authentication, Model model, PageRequestDTO pageRequestDTO) {
        String currentUserRole = getCurrentUserRole(authentication);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        return "board/register";
    }

    @PostMapping("/register")
    public String registerPOST(BoardDTO boardDTO, Authentication authentication, RedirectAttributes redirectAttributes) {
        Long currentUserId = getCurrentUserId(authentication);
        String currentUserRole = getCurrentUserRole(authentication);
        boardDTO.setUserId(currentUserId);
        Long boardId = boardService.register(boardDTO, currentUserRole);

        redirectAttributes.addFlashAttribute("result", boardId);

        if(boardDTO.getBoardType() != null && !boardDTO.getBoardType().isEmpty()) {
            redirectAttributes.addAttribute("boardType", boardDTO.getBoardType());
        }
        return "redirect:/board/list";
    }

    @GetMapping("/modify")
    public void modify(Long boardId, PageRequestDTO pageRequestDTO, Model model, Authentication authentication) {
        BoardDTO boardDTO = boardService.getBoardOnly(boardId);
        String currentUserRole = getCurrentUserRole(authentication);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("dto", boardDTO);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResponseEntity<String> modifyPOST(@Valid BoardDTO boardDTO, BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("입력값이 올바르지 않습니다.");
        }

        try {
            Long currentUserId = getCurrentUserId(authentication);
            BoardDTO existingBoard = boardService.getBoardOnly(boardDTO.getBoardId());

            if (existingBoard.getUserId() == null || !existingBoard.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 권한이 없습니다.");
            }

            boardDTO.setUserId(currentUserId);
            boardService.modify(boardDTO);
            return ResponseEntity.ok("success");

        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류: " + e.getMessage());
        }
    }
    // 게시글 삭제
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<String> removePOST(@RequestParam("boardId") Long boardId, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            String currentUserRole = getCurrentUserRole(authentication);

            boardService.remove(boardId, currentUserId, currentUserRole);
            return ResponseEntity.ok("success");

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PutMapping("/admin/{boardId}/blind")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public ResponseEntity<String> blindPost(@PathVariable Long boardId) {
        try {
            boardService.toggleBlind(boardId);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/like")
    @ResponseBody
    public ResponseEntity<String> likePOST(@RequestParam("boardId") Long boardId, Authentication authentication){
        try {
            Long currentUserId = getCurrentUserId(authentication);
            boardService.toggleLike(boardId, currentUserId);

            return ResponseEntity.ok("");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다.");
        }
    }

    @PostMapping("/report")
    @ResponseBody
    public ResponseEntity<String> reportPOST(@RequestParam("boardId") Long boardId, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            boardService.report(boardId, currentUserId);

            return ResponseEntity.ok("신고가 접수되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생하였습니다.");
        }
    }



}

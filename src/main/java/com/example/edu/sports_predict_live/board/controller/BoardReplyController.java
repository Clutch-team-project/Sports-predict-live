package com.example.edu.sports_predict_live.board.controller;

import com.example.edu.sports_predict_live.board.dto.BoardReplyDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.service.BoardReplyService;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class BoardReplyController {
    private final BoardReplyService boardReplyService;
    private final UserRepository userRepository;

    // 현재 로그인 한 유저의 ID 가져오기
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인 후 이용 가능합니다.");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            User user = userRepository.findByLoginIdAndDeletedAtIsNull(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
            return user.getUserId();
        }
    }

    // 특정 게시글의 댓글 목록 조회
    @GetMapping("/list/{boardId}")
    public ResponseEntity<PageResponseDTO<BoardReplyDTO>> getList(
            @PathVariable("boardId") Long boardId,
            PageRequestDTO pageRequestDTO,
            Authentication authentication) {
        Long currentUserId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                currentUserId = getCurrentUserId(authentication);
            } catch (Exception e) {
                // 목록 조회 시 비회원 또는 ID 파싱 에러는 null 처리하여 계속 진행
            }
        }
        PageResponseDTO<BoardReplyDTO> responseDTO = boardReplyService.getListOfBoard(boardId, pageRequestDTO, currentUserId);

        return ResponseEntity.ok(responseDTO);
    }

    // 댓글 등록
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> register(@RequestBody BoardReplyDTO replyDTO, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            replyDTO.setUserId(currentUserId);
            Long replyId = boardReplyService.register(replyDTO);
            return ResponseEntity.ok(Map.of("replyId", replyId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // 댓글 삭제
    @DeleteMapping("/{replyId}")
    public ResponseEntity<String> remove(@PathVariable("replyId") Long replyId, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            String currentUserRole = authentication.getAuthorities().iterator().next().getAuthority();
            boardReplyService.removeReply(replyId, currentUserId, currentUserRole);
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 중 오류가 발생했습니다.");
        }
    }

    // 댓글 수정
    @PutMapping("/{replyId}")
    public ResponseEntity<String> modify(@PathVariable("replyId") Long replyId, @RequestBody BoardReplyDTO boardReplyDTO, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            boardReplyDTO.setReplyId(replyId);
            boardReplyService.modifyReply(boardReplyDTO, currentUserId);
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("수정 중 오류가 발생했습니다.");
        }
    }

    // 댓글 좋아요 토글
    @PostMapping("/like")
    public ResponseEntity<String> likeReply(@RequestBody Map<String, Long> payload, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            Long replyId = payload.get("replyId");
            boardReplyService.toggleLikeReply(replyId, currentUserId);
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // 댓글 신고
    @PostMapping("/report")
    public ResponseEntity<String> reportReply(@RequestBody Map<String, Long> payload, Authentication authentication) {
        try {
            Long currentUserId = getCurrentUserId(authentication);
            Long replyId = payload.get("replyId");
            boardReplyService.reportReply(replyId, currentUserId);
            return ResponseEntity.ok("신고가 접수되었습니다.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("로그인")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다.");
        }
    }

    // 댓글 블라인드 토글 (관리자 전용)
    @PutMapping("/admin/{replyId}/blind")
    public ResponseEntity<String> blindReply(@PathVariable("replyId") Long replyId,
                                             Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            String currentUserRole = authentication.getAuthorities().iterator().next().getAuthority();
            boardReplyService.toggleBlindReply(replyId, currentUserRole);
            return ResponseEntity.ok("success");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류가 발생했습니다.");
        }
    }
}

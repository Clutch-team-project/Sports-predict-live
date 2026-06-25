package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.dto.BoardReplyDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.entity.BoardReply;
import com.example.edu.sports_predict_live.board.entity.BoardReplyLike;
import com.example.edu.sports_predict_live.board.entity.BoardReplyReport;
import com.example.edu.sports_predict_live.board.repository.BoardReplyLikeRepository;
import com.example.edu.sports_predict_live.board.repository.BoardReplyReportRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardReplyServiceImpl implements BoardReplyService {
    private final BoardReplyRepository boardReplyRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardReplyLikeRepository boardReplyLikeRepository;
    private final BoardReplyReportRepository boardReplyReportRepository;
    private final AiModerationService aiModerationService;

    private final ModelMapper modelMapper;

    // 댓글 등록 (ai 필터링)
    @Override
    @Transactional
    public Long register(BoardReplyDTO boardReplyDTO) {
        Board board = boardRepository.findById(boardReplyDTO.getBoardId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        if (board.getDeletedAt() != null) {
            throw new IllegalStateException("삭제된 게시글에는 댓글을 등록할 수 없습니다.");
        }

        User user = userRepository.findById(boardReplyDTO.getUserId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        String nickName = (user.getNickname() != null && !user.getNickname().trim().isEmpty() ? user.getNickname() : "익명");

        BoardReply reply = BoardReply.builder()
                .board(board)
                .replyText(boardReplyDTO.getReplyText())
                .userId(boardReplyDTO.getUserId())
                .nickname(nickName)
                .isBlinded(false)
                .build();

        BoardReply saveReply = boardReplyRepository.saveAndFlush(reply);
        aiModerationService.checkAndReplyAsync(saveReply.getReplyId(), saveReply.getReplyText());

        return saveReply.getReplyId();
    }

    // 댓글 조회
    @Override
    public BoardReplyDTO read(Long replyId) {
        BoardReply reply = boardReplyRepository.findById(replyId).orElseThrow();
        return modelMapper.map(reply, BoardReplyDTO.class);
    }

    // 댓글 수정
    @Override
    public void modifyReply(BoardReplyDTO boardReplyDTO, Long currentUserId) {
        BoardReply reply = boardReplyRepository.findById(boardReplyDTO.getReplyId()).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        if (reply.getDeletedAt() != null) {
            throw new IllegalStateException("이미 삭제된 댓글입니다.");
        }
        if (!reply.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("댓글 수정 권한이 없습니다.");
        }
        reply.changeText(boardReplyDTO.getReplyText());
        boardReplyRepository.save(reply);
    }

    // 댓글 삭제
    @Override
    public void removeReply(Long replyId, Long currentUserId, String currentUserRole) {
        BoardReply reply = boardReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        if(reply.getUserId().equals(currentUserId) || "ROLE_ADMIN".equals(currentUserRole)) {
            reply.softDelete();
            boardReplyRepository.save(reply);
        } else {
            throw new AccessDeniedException("댓글 삭제 권한이 없습니다.");
        }
    }

    // 특정 게시글의 페이징된 댓글 목록 조회(좋아요, 신고 포함)
    @Override
    public PageResponseDTO<BoardReplyDTO> getListOfBoard(Long boardId, PageRequestDTO pageRequestDTO, Long currentUserId) {

        Sort sortOrder = Sort.by("replyId").descending();

        if ("like".equals(pageRequestDTO.getSort())) {
            sortOrder = Sort.by("likeCount").descending().and(Sort.by("replyId").descending());
        } else if ("latest".equals(pageRequestDTO.getSort())) {
            sortOrder = Sort.by("replyId").descending();
        }

        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() <= 0 ? 0 : pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                sortOrder
        );

        Page<BoardReply> result = boardReplyRepository.findByBoard_BoardIdAndDeletedAtIsNull(boardId, pageable);
        List<BoardReply> replies = result.getContent();

        List<Long> replyIds = replies.stream().map(BoardReply::getReplyId).collect(Collectors.toList());

        java.util.Set<Long> likedReplyIds = new java.util.HashSet<>();
        java.util.Set<Long> reportedReplyIds = new java.util.HashSet<>();

        if (currentUserId != null && !replyIds.isEmpty()) {
            List<BoardReplyLike> likes = boardReplyLikeRepository.findByBoardReply_ReplyIdInAndUserId(replyIds, currentUserId);
            likes.forEach(like -> likedReplyIds.add(like.getBoardReply().getReplyId()));

            List<BoardReplyReport> reports = boardReplyReportRepository.findByBoardReply_ReplyIdInAndUserId(replyIds, currentUserId);
            reports.forEach(report -> reportedReplyIds.add(report.getBoardReply().getReplyId()));
        }

        List<BoardReplyDTO> dtoList = replies.stream()
                .map(reply -> BoardReplyDTO.builder()
                        .replyId(reply.getReplyId())
                        .boardId(boardId)
                        .replyText(reply.getReplyText())
                        .userId(reply.getUserId())
                        .nickname(reply.getNickname())
                        .createdAt(reply.getCreatedAt())
                        .likeCount(reply.getLikeCount())
                        .isBlinded(reply.isBlinded())
                        .isLiked(likedReplyIds.contains(reply.getReplyId()))
                        .isReported(reportedReplyIds.contains(reply.getReplyId()))
                        .build())
                .collect(Collectors.toList());

        return PageResponseDTO.<BoardReplyDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total((int) result.getTotalElements())
                .build();
    }

    // 댓글 좋아요 토글
    @Override
    @Transactional
    public void toggleLikeReply(Long replyId, Long currentUserId) {
        BoardReply reply = boardReplyRepository.findById(replyId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        if (reply.getDeletedAt() != null) {
            throw new IllegalStateException("삭제된 댓글에는 좋아요를 누를 수 없습니다.");
        }
        boardReplyLikeRepository.findByBoardReply_ReplyIdAndUserId(replyId, currentUserId).ifPresentOrElse(
                like -> {
                    boardReplyLikeRepository.delete(like);
                    reply.changeLikeCount(reply.getLikeCount() - 1);
                },
                () -> {
                    BoardReplyLike newLike = BoardReplyLike.builder()
                            .boardReply(reply)
                            .userId(currentUserId)
                            .build();
                    boardReplyLikeRepository.save(newLike);
                    reply.changeLikeCount(reply.getLikeCount() + 1);
                }
            );
        boardReplyRepository.save(reply);
    }

    // 댓글 신고
    @Override
    @Transactional
    public void reportReply(Long replyId, Long currentUserId) {
        BoardReply reply = boardReplyRepository.findById(replyId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        if (reply.getDeletedAt() != null) {
            throw new IllegalStateException("삭제된 댓글은 신고할 수 없습니다.");
        }
        if (boardReplyReportRepository.findByBoardReply_ReplyIdAndUserId(replyId, currentUserId).isPresent()) {
            throw new IllegalArgumentException("이미 신고한 댓글입니다.");
        }
        BoardReplyReport report = BoardReplyReport.builder()
                .boardReply(reply)
                .userId(currentUserId)
                .build();
        boardReplyReportRepository.save(report);
    }

    // 댓글 블라인드 여부 토글 (관리자 전용)
    @Override
    @Transactional
    public void toggleBlindReply(Long replyId, String currentUserRole) {
        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            throw new org.springframework.security.access.AccessDeniedException("관리자만 접근 가능한 기능입니다.");
        }
        BoardReply reply = boardReplyRepository.findById(replyId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        if (reply.getDeletedAt() != null) {
            throw new IllegalStateException("삭제된 댓글은 블라인드 처리할 수 없습니다.");
        }
        reply.changeBlind(!reply.isBlinded());
        boardReplyRepository.save(reply);
    }
}

package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.dto.BoardDTO;
import com.example.edu.sports_predict_live.board.dto.BoardListAllDTO;
import com.example.edu.sports_predict_live.board.dto.PageRequestDTO;
import com.example.edu.sports_predict_live.board.dto.PageResponseDTO;
import com.example.edu.sports_predict_live.board.entity.BoardLike;
import com.example.edu.sports_predict_live.board.entity.BoardReport;
import com.example.edu.sports_predict_live.board.repository.BoardLikeRepository;
import com.example.edu.sports_predict_live.board.repository.BoardReportRepository;
import com.example.edu.sports_predict_live.board.repository.BoardRepository;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class BoardServiceImpl implements BoardService{
    private final ModelMapper modelMapper;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final BoardReportRepository boardReportRepository;
    private final AiModerationService aiModerationService;

    // 파일 저장 경로
    @Value("${com.example.upload.path}")
    private String uploadPath;

    // 게시글 생성
    public Long register(BoardDTO boardDTO, String currentUserRole) {
        if("공지".equals(boardDTO.getCategory()) && !currentUserRole.equals("ROLE_ADMIN")) {
            throw new AccessDeniedException("공지사항은 관리자만 작성할 수 있습니다.");
        }

        if (boardDTO.getBoardType() != null) {
            String trimmed = boardDTO.getBoardType().trim().toLowerCase();
            if (trimmed.equals("football") || trimmed.equals("soccer")) {
                boardDTO.setBoardType("soccer");
            }
        }

        boardDTO.setNotice("공지".equals(boardDTO.getCategory()));
        boardDTO.setBlinded(false);

        Board board = dtoToEntity(boardDTO);

        if(boardDTO.getFiles() != null && !boardDTO.getFiles().isEmpty()) {
            String absolutePath = java.nio.file.Paths.get(uploadPath).toAbsolutePath().toString();
            java.io.File uploadDir = new java.io.File(absolutePath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            for (org.springframework.web.multipart.MultipartFile multipartFile : boardDTO.getFiles()) {
                if (multipartFile.isEmpty()) continue;

                String originalName = multipartFile.getOriginalFilename();
                String uuid = java.util.UUID.randomUUID().toString();
                String saveName = uuid + "_" + originalName;
                java.nio.file.Path savePath = java.nio.file.Paths.get(absolutePath, saveName);

                try {
                    multipartFile.transferTo(savePath);
                    board.addImage(uuid, originalName);
                } catch (java.io.IOException e) {
                    throw new RuntimeException("이미지 저장 중 에러 발생", e);
                }
            }
        }

        Long boardId = boardRepository.save(board).getBoardId();

        String textToAnalyze = boardDTO.getTitle() + " " + boardDTO.getContent();
        aiModerationService.checkAndBlindAsync(boardId, textToAnalyze);
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
        boolean isNoticeFlag = "공지".equals(boardDTO.getCategory());
        boardDTO.setNotice(isNoticeFlag);
        board.change(boardDTO.getTitle(), boardDTO.getContent(), boardDTO.getCategory(), boardDTO.isNotice());

        board.clearImage();

        // 기존에 있던 파일명 다시 담기
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

        // 새로 추가된 파일 저장하기
        if(boardDTO.getFiles() != null && !boardDTO.getFiles().isEmpty()) {
            String absolutePath = java.nio.file.Paths.get(uploadPath).toAbsolutePath().toString();
            java.io.File uploadDir = new java.io.File(absolutePath);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            for (org.springframework.web.multipart.MultipartFile multipartFile : boardDTO.getFiles()) {
                if (multipartFile.isEmpty()) continue;

                String originalName = multipartFile.getOriginalFilename();
                String uuid = java.util.UUID.randomUUID().toString();
                String saveName = uuid + "_" + originalName;
                java.nio.file.Path savePath = java.nio.file.Paths.get(absolutePath, saveName);

                try {
                    multipartFile.transferTo(savePath);
                    board.addImage(uuid, originalName);
                } catch (java.io.IOException e) {
                    throw new RuntimeException("새 이미지 저장 중 에러 발생", e);
                }
            }
        }
        boardRepository.save(board);
    }
    // 게시글 삭제
    @Override
    public void remove(Long boardId, Long currentUserId, String currentUserRole) {
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        boolean isOwner = board.getUserId().equals(currentUserId);
        boolean isAdmin = "ROLE_ADMIN".equals(currentUserRole);

        if(!isOwner && !isAdmin) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        board.softDelete();
        boardRepository.save(board);
    }
    // 리스트 조회
    @Override
    public PageResponseDTO<BoardListAllDTO> listWithAll(PageRequestDTO pageRequestDTO) {
        String[] types = pageRequestDTO.getTypes();
        String keyword = pageRequestDTO.getKeyword();
        String category = pageRequestDTO.getCategory();
        String sort = pageRequestDTO.getSort();
        Pageable pageable = pageRequestDTO.getPageable("boardId");
        String boardType = pageRequestDTO.getBoardType();

        Page<BoardListAllDTO> result = boardRepository.searchWithAll(types, keyword, category, sort, pageable, boardType);

        List<BoardListAllDTO> dtoList = new ArrayList<>(result.getContent());

        PageRequest noticePageable = PageRequest.of(0, 5);
        Page<BoardListAllDTO> noticeResult = boardRepository.searchWithAll(types, keyword, "공지", null, noticePageable, boardType);
        List<BoardListAllDTO> noticeDtoList = new ArrayList<>(noticeResult.getContent());
        dtoList.removeIf(dto -> "공지".equals(dto.getCategory()));
        noticeDtoList.addAll(dtoList);
        dtoList = noticeDtoList;

        int totalElements = (int)result.getTotalElements();

        if (totalElements == 0 && !dtoList.isEmpty()) {
            totalElements = dtoList.size();
        }
        return PageResponseDTO.<BoardListAllDTO>withAll()
                .pageRequestDTO(pageRequestDTO)
                .dtoList(dtoList)
                .total(totalElements)
                .build();
    }
    // 좋아요 토글
    @Override
    public void toggleLike(Long boardId, Long userId) {
        Optional<Board> result = boardRepository.findById(boardId);
        Board board = result.orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        Optional<BoardLike> boardLikeOp = boardLikeRepository.findByBoard_BoardIdAndUserId(boardId, userId);
        if(boardLikeOp.isPresent()) {
            boardLikeRepository.delete(boardLikeOp.get());
            board.changeLikeCount(board.getLikeCount() - 1);
        } else {
            BoardLike boardLike = BoardLike.builder()
                    .board(board)
                    .userId(userId)
                    .build();
            boardLikeRepository.save(boardLike);
            board.changeLikeCount(board.getLikeCount() + 1);
        }
        boardRepository.save(board);
        boardRepository.flush();
    }
    // 좋아요 여부 확인
    @Override
    public boolean checkIsLiked(Long boardId, Long userId) {
        if(userId == null){
            return false;
        }
        return boardLikeRepository.findByBoard_BoardIdAndUserId(boardId, userId).isPresent();
    }
    // 신고 기능
    @Override
    @Transactional
    public void report(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        if (board.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 게시글은 신고할 수 없습니다.");
        }
        if(board.isNotice()) {
            throw new IllegalStateException("공지글은 신고할 수 없스니다.");
        }

        Optional<BoardReport> existingReport = boardReportRepository.findByBoard_BoardIdAndUserId(boardId, userId);
        if (existingReport.isPresent()) {
            throw new IllegalStateException("이미 신고가 접수된 게시글입니다.");
        }
        BoardReport newReport = BoardReport.builder()
                .board(board)
                .userId(userId)
                .build();

        boardReportRepository.save(newReport);
    }
    // 신고 여부 확인
    @Override
    public boolean checkIsReported(Long boardId, Long userId) {
        if(userId == null) return false;
        Board board = Board.builder()
                .boardId(boardId)
                .build();
        return boardReportRepository.findByBoard_BoardIdAndUserId(board.getBoardId(), userId).isPresent();
    }
    // 게시글 블라인드 처리
    @Override
    public void toggleBlind(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        board.changeBlind(!board.isBlinded());

        boardRepository.save(board);
    }

    // 메인홈 일별 인기글 5개 출력
    @Override
    public List<BoardDTO> findTop5ViewCountToday(String boardType) {
        if (boardType != null) {
            String normalized = boardType.trim().toLowerCase();
            if (normalized.equals("football") || normalized.equals("soccer")) {
                boardType = "soccer"; // "football"이나 "soccer"로 들어와도 "soccer"로 통일
            } else if (normalized.equals("baseball")) {
                boardType = "baseball";
            } else if (normalized.equals("lol")) {
                boardType = "lol";
            }
        }
        List<BoardListAllDTO> result = boardRepository.findPopularPosts(5, boardType);

        return result.stream()
                .map(listAllDTO -> {
                    BoardDTO dto = new BoardDTO();
                    dto.setBoardId(listAllDTO.getBoardId());
                    dto.setTitle(listAllDTO.getTitle());
                    dto.setNickname(listAllDTO.getNickname());
                    dto.setViewCount(listAllDTO.getViewCount());
                    dto.setCategory(listAllDTO.getCategory());
                    dto.setCreatedAt(listAllDTO.getCreatedAt());
                    dto.setBoardType(listAllDTO.getBoardType());
                    return dto;
                })
                .collect(Collectors.toList());
        }
}

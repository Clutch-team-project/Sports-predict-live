package com.example.edu.sports_predict_live.board.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {

    private Long boardId; // 게시글 ID
    private Long userId; // 작성한 유저 ID

    @NotEmpty
    private String category; // 카테고리

    @NotEmpty
    @Size(min = 1, max = 100)
    private String title; // 제목

    @NotEmpty
    private String content; // 내용

    private int viewCount; // 조회수
    private int likeCount; // 좋아요수
    private boolean isNotice; // 공지 고정 여부
    private boolean isBlinded; // 블라인드 여부

    // BaseEntity에서 자동으로 채워지는 것들
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시

    private List<String> fileNames; // 첨부 파일명
}

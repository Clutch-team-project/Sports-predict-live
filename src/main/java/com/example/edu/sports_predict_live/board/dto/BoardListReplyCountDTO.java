package com.example.edu.sports_predict_live.board.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoardListReplyCountDTO {
    private Long boardId;
    private Long userId;
    private String category;
    private String title;
    private int viewCount;
    private int likeCount;
    private boolean isNotice;
    private boolean isBlinded;

    private LocalDateTime createdAt; // 등록일
    private LocalDateTime updatedAt; // 수정일 (수정시 작성일이 수정일로 바뀌어서 표기됨)

    private Long replyCount; // 가져올 총 댓글 수
}
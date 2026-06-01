package com.example.edu.sports_predict_live.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoardListAllDTO {
    private Long boardId;
    private Long userId;
    private String category;
    private String title;
    private int viewCount;
    private int likeCount;
    private boolean isNotice;
    private boolean isBlinded;
    private boolean isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long replyCount;

    private List<String> fileNames; // 첨부파일 이름 목록('uuid_파일명' 형태)
}

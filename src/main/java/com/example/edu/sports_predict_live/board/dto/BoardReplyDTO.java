package com.example.edu.sports_predict_live.board.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardReplyDTO {
    private Long replyId;
    private Long boardId;
    private String replyText;
    private Long userId;
    private String nickname;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;
    private int likeCount;
    private boolean isLiked;
    private boolean isReported;
    private boolean isBlinded;
    private Long parentId;
    @Builder.Default
    private java.util.List<BoardReplyDTO> children = new java.util.ArrayList<>();
}

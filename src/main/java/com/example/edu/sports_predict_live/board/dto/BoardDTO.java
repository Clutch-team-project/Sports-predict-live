package com.example.edu.sports_predict_live.board.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {

    private Long boardId;
    private Long userId;

    private String loginId;
    private String nickname;

    @NotEmpty
    private String category;

    @NotEmpty
    @Size(min = 1, max = 100)
    private String title;

    @NotEmpty
    private String content;

    private int viewCount;
    private int likeCount;
    private int replyCount;

    private boolean liked;
    private boolean isNotice;
    private boolean isBlinded;
    private boolean isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<MultipartFile> files;
    private List<String> fileNames;

    private String boardType;
}

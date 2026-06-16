package com.example.edu.sports_predict_live.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "imageSet")
public class Board extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    private Long userId;

    @Column(length = 20, nullable = false)
    private String category;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private boolean isNotice;

    @Column(nullable = false)
    private boolean isBlinded;

    private LocalDateTime deletedAt;

    // 이미지 연관관계 설정
    @OneToMany(mappedBy = "board",
    cascade = {CascadeType.ALL},
    fetch = FetchType.LAZY,
    orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 20)
    private Set<BoardImage> imageSet = new HashSet<>();

    // 게시글 정보 수정
    public void change(String title, String content, String category, boolean isNotice) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.isNotice = isNotice;
    }

    public void changeViewCount(int viewCount) {
        this.boardId = boardId;
        this.viewCount = viewCount;
    }

    public void changeLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 이미지 추가
    public void addImage(String uuid, String fileName) {
        if(this.imageSet.size() >= 5) {
            throw new IllegalArgumentException("이미지는 최대 5장 까지만 첨부할 수 있습니다.");
        }

        BoardImage boardImage = BoardImage.builder()
                .uuid(uuid)
                .fileName(fileName)
                .board(this)
                .ord(imageSet.size())
                .build();
        imageSet.add(boardImage);
    }

    // 이미지 전체 초기화
    public void clearImage() {
        imageSet.forEach(boardImage -> boardImage.changeBoard(null));
        this.imageSet.clear();
    }
}


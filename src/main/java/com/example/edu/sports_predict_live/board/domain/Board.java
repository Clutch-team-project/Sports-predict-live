package com.example.edu.sports_predict_live.board.domain;

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
    private Long boardId; // 게시글 ID

    private Long userId; // 작성한 유저 ID

    @Column(length = 20, nullable = false)
    private String category; // 카테고리

    @Column(length = 200, nullable = false)
    private String title; // 제목

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 내용

    @Column(nullable = false)
    private int viewCount; // 조회수

    @Column(nullable = false)
    private int likeCount; // 좋아요수

    @Column(nullable = false)
    private boolean isNotice; // 공지 고정 여부

    @Column(nullable = false)
    private boolean isBlinded; // 블라인드 여부

    private LocalDateTime deletedAt; // 삭제일시(소프트 딜리트용)

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

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void addImage(String uuid, String fileName) {

        if(this.imageSet.size() >= 5) {
            throw new IllegalArgumentException("이미지는 최대 5장 까지만 첨부할 수 있습니다.");
        }

        BoardImage boardImage = BoardImage.builder()
                .uuid(uuid)
                .fileName(fileName)
                .board(this)
                .ord(imageSet.size()) // 현재 저당 된 개수 기반 순서 자동 부여(0, 1, 2...)
                .build();
        imageSet.add(boardImage);
    }

    // 이미지 전체 초기화
    public void clearImage() {
        imageSet.forEach(boardImage -> boardImage.changeBoard(null));
        this.imageSet.clear();
    }
}

package com.example.edu.sports_predict_live.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "board")
@Table(name = "board_reply", indexes = {
        @Index(name = "idx_reply_board_id", columnList = "board_id")
})
public class BoardReply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false, length = 1000)
    private String replyText;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BoardReply parent;

    public void setParent(BoardReply parent) {
        this.parent = parent;
    }

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "integer default 0")
    private int likeCount;

    public void changeLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    @Column(columnDefinition = "boolean default false")
    private boolean isBlinded;

    public void changeBlind(boolean isBlinded) {
        this.isBlinded = isBlinded;
    }

    public void changeText(String text) {
        this.replyText = text;
    }

    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
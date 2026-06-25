package com.example.edu.sports_predict_live.livematch.comment.entity;

import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_comment",
        indexes = {
                @Index(name = "idx_match_comment_match_created", columnList = "match_id, created_at"),
                @Index(name = "idx_match_comment_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_team_id")
    private Team supportTeam;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MatchComment(Match match, User user, Team supportTeam, String content) {
        this.match = match;
        this.user = user;
        this.supportTeam = supportTeam;
        this.content = content;
    }

    public void delete() {
        this.deleted = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

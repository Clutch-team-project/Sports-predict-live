package com.example.edu.sports_predict_live.user.entity;

import com.example.edu.sports_predict_live.team.entity.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 관심 팀 — 유저:팀 다대다 연결 (유저당 같은 팀 중복 등록 불가)
@Entity
@Table(name = "user_favorite_team",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "team_id"}))
@Getter
@NoArgsConstructor
public class UserFavoriteTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UserFavoriteTeam(User user, Team team) {
        this.user = user;
        this.team = team;
    }
}

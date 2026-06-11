package com.example.edu.sports_predict_live.team.entity;

import com.example.edu.sports_predict_live.sport.entity.Sport;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 팀 기본 정보 — LOL 팀의 name은 lolesports 코드 값(T1, GEN 등)으로 저장
// (sport_id, name) 유니크 — init SQL 재실행 시 중복 등록 방지
@Entity
@Table(
        name = "team",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_sport_name",
                columnNames = {"sport_id", "name"}
        )
)
@Getter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_id")
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "emblem_url", length = 500)
    private String emblemUrl;
}

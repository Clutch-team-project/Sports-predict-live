package com.example.edu.sports_predict_live.player.repository;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatLol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerSeasonStatLolRepository extends JpaRepository<PlayerSeasonStatLol, Long> {

    // KDA 내림차순 선수 기록 조회
    @Query("""
        SELECT l FROM PlayerSeasonStatLol l
        JOIN FETCH l.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.sport.code = 'lol'
          AND pss.season = :season
        ORDER BY l.kda DESC NULLS LAST
    """)
    List<PlayerSeasonStatLol> findBySeasonOrderByKdaDesc(@Param("season") String season);

    // AI 예측용 — 팀 이름으로 선수 성적 조회 (KDA 내림차순)
    @Query("""
        SELECT l FROM PlayerSeasonStatLol l
        JOIN FETCH l.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.name = :teamName
          AND pss.season = :season
        ORDER BY l.kda DESC NULLS LAST
    """)
    List<PlayerSeasonStatLol> findByTeamNameAndSeason(
            @Param("teamName") String teamName,
            @Param("season") String season
    );
}

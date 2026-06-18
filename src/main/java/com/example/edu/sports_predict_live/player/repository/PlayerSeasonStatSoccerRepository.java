package com.example.edu.sports_predict_live.player.repository;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatSoccer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerSeasonStatSoccerRepository extends JpaRepository<PlayerSeasonStatSoccer, Long> {

    // 득점 내림차순 선수 기록 조회
    @Query("""
        SELECT s FROM PlayerSeasonStatSoccer s
        JOIN FETCH s.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.sport.code = 'soccer'
          AND pss.season = :season
        ORDER BY s.goals DESC, s.assists DESC
    """)
    List<PlayerSeasonStatSoccer> findBySeasonOrderByGoalsDesc(@Param("season") String season);

    // AI 예측용 — 팀별 상위 득점·도움 선수 조회
    @Query("""
        SELECT s FROM PlayerSeasonStatSoccer s
        JOIN FETCH s.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.teamId = :teamId
          AND pss.season = :season
        ORDER BY s.goals DESC, s.assists DESC
    """)
    List<PlayerSeasonStatSoccer> findTopByTeamAndSeason(
            @Param("teamId") Long teamId,
            @Param("season") String season
    );
}
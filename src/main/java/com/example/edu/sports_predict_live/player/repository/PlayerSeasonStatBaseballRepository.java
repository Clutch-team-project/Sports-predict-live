package com.example.edu.sports_predict_live.player.repository;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerSeasonStatBaseballRepository extends JpaRepository<PlayerSeasonStatBaseball, Long> {

    // 타자 기록 조회 (battingAvg 기준 내림차순)
    @Query("""
        SELECT b FROM PlayerSeasonStatBaseball b
        JOIN FETCH b.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.sport.code = :sportCode
          AND pss.season = :season
          AND b.battingAvg IS NOT NULL
        ORDER BY b.battingAvg DESC
    """)
    List<PlayerSeasonStatBaseball> findHittersBySportAndSeason(
            @Param("sportCode") String sportCode,
            @Param("season") String season
    );

    // 투수 기록 조회 (era 기준 오름차순)
    @Query("""
        SELECT b FROM PlayerSeasonStatBaseball b
        JOIN FETCH b.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.sport.code = :sportCode
          AND pss.season = :season
          AND b.era IS NOT NULL
        ORDER BY b.era ASC
    """)
    List<PlayerSeasonStatBaseball> findPitchersBySportAndSeason(
            @Param("sportCode") String sportCode,
            @Param("season") String season
    );

    Optional<PlayerSeasonStatBaseball> findByPlayerSeasonStatPlayerSeasonStatId(Long playerSeasonStatId);

    // AI 예측용 — 선발 투수 이름으로 시즌 성적 조회
    @Query("""
        SELECT b FROM PlayerSeasonStatBaseball b
        JOIN FETCH b.playerSeasonStat pss
        JOIN FETCH pss.player p
        WHERE p.name = :name
          AND pss.season = :season
          AND b.era IS NOT NULL
    """)
    Optional<PlayerSeasonStatBaseball> findPitcherByNameAndSeason(
            @Param("name") String name,
            @Param("season") String season
    );
}

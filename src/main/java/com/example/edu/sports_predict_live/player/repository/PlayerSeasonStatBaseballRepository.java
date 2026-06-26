package com.example.edu.sports_predict_live.player.repository;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayerSeasonStatBaseballRepository extends JpaRepository<PlayerSeasonStatBaseball, Long> {

    // 타자 기록 조회 (battingAvg 기준 내림차순) — 투수 포지션 제외
    @Query("""
        SELECT b FROM PlayerSeasonStatBaseball b
        JOIN FETCH b.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.sport.code = :sportCode
          AND pss.season = :season
          AND b.battingAvg IS NOT NULL
          AND NOT (UPPER(p.position) IN ('RHP','LHP','SP','RP','P')
                   OR p.position LIKE '%투수%'
                   OR p.position LIKE '%우완%'
                   OR p.position LIKE '%좌완%')
        ORDER BY b.battingAvg DESC
    """)
    List<PlayerSeasonStatBaseball> findHittersBySportAndSeason(
            @Param("sportCode") String sportCode,
            @Param("season") String season
    );

    // 투수 기록 조회 (era 기준 오름차순) — 투수 포지션 기준, era NULL(이닝 미달)도 팀 필터 용도로 포함
    @Query("""
        SELECT b FROM PlayerSeasonStatBaseball b
        JOIN FETCH b.playerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        WHERE t.sport.code = :sportCode
          AND pss.season = :season
          AND (UPPER(p.position) IN ('RHP','LHP','SP','RP','P')
               OR p.position LIKE '%투수%'
               OR p.position LIKE '%우완%'
               OR p.position LIKE '%좌완%')
        ORDER BY CASE WHEN b.era IS NULL THEN 1 ELSE 0 END ASC, b.era ASC
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

    @Query("""
        SELECT b FROM PlayerSeasonStatBaseball b
        JOIN FETCH b.playerSeasonStat pss
        JOIN FETCH pss.player p
        WHERE p.playerId IN :playerIds
          AND pss.season = :season
    """)
    List<PlayerSeasonStatBaseball> findByPlayerIdsAndSeason(
            @Param("playerIds") Collection<Long> playerIds,
            @Param("season") String season
    );
}

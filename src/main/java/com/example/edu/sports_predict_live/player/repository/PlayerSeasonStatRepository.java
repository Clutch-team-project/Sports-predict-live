package com.example.edu.sports_predict_live.player.repository;

import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerSeasonStatRepository extends JpaRepository<PlayerSeasonStat, Long> {

    // 종목 코드 + 시즌으로 선수 기록 전체 조회
    @Query("""
        SELECT pss FROM PlayerSeasonStat pss
        JOIN FETCH pss.player p
        JOIN FETCH p.team t
        JOIN FETCH t.sport s
        WHERE s.code = :sportCode
          AND pss.season = :season
    """)
    List<PlayerSeasonStat> findBySportCodeAndSeason(
            @Param("sportCode") String sportCode,
            @Param("season") String season
    );

    // 특정 선수 + 시즌 조회
    @Query("""
        SELECT pss FROM PlayerSeasonStat pss
        JOIN FETCH pss.player p
        WHERE p.playerId = :playerId
          AND pss.season = :season
    """)
    Optional<PlayerSeasonStat> findByPlayerIdAndSeason(
            @Param("playerId") Long playerId,
            @Param("season") String season
    );
}

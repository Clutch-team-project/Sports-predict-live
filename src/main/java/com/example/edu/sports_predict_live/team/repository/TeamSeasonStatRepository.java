package com.example.edu.sports_predict_live.team.repository;

import com.example.edu.sports_predict_live.team.entity.TeamSeasonStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamSeasonStatRepository extends JpaRepository<TeamSeasonStat, Long> {

    // 종목 코드 + 시즌으로 팀 순위 조회 (rank 오름차순)
    @Query("""
        SELECT tss FROM TeamSeasonStat tss
        JOIN FETCH tss.team t
        JOIN FETCH tss.sport s
        WHERE s.code = :sportCode
          AND tss.season = :season
        ORDER BY tss.rank ASC NULLS LAST
    """)
    List<TeamSeasonStat> findBySportCodeAndSeason(
            @Param("sportCode") String sportCode,
            @Param("season") String season
    );
}

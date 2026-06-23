package com.example.edu.sports_predict_live.team.repository;

import com.example.edu.sports_predict_live.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // 종목 코드로 팀 목록 조회 (LOL 순위표 이름→ID 매핑용)
    @Query("""
        SELECT t FROM Team t
        JOIN FETCH t.sport s
        WHERE s.code = :sportCode
        ORDER BY t.name ASC
    """)
    List<Team> findBySportCode(@Param("sportCode") String sportCode);

    @Modifying
    @Query("UPDATE Team t SET t.emblemUrl = :url WHERE t.teamId = :teamId")
    void updateEmblemUrl(@Param("teamId") Long teamId, @Param("url") String url);

    @Modifying
    @Query("UPDATE Team t SET t.emblemUrl = :url WHERE t.name = :name")
    void updateEmblemUrlByName(@Param("name") String name, @Param("url") String url);
}

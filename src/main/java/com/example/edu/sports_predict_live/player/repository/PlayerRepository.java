package com.example.edu.sports_predict_live.player.repository;

import com.example.edu.sports_predict_live.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // 종목 코드로 선수 목록 조회
    @Query("""
        SELECT p FROM Player p
        JOIN FETCH p.team t
        JOIN FETCH t.sport s
        WHERE s.code = :sportCode
        ORDER BY t.name ASC, p.name ASC
    """)
    List<Player> findBySportCode(@Param("sportCode") String sportCode);
}

package com.example.edu.sports_predict_live.livematch.stat.repository;

import com.example.edu.sports_predict_live.livematch.stat.entity.MatchPlayerStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerStatRepository extends JpaRepository<MatchPlayerStat, Long> {

    List<MatchPlayerStat> findByMatchIdOrderByTeamIdAscPlayerIdAscStatKeyAsc(Long matchId);
}

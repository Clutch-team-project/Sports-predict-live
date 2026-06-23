package com.example.edu.sports_predict_live.livematch.repository;

import com.example.edu.sports_predict_live.livematch.entity.MatchPlayerStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchPlayerStatRepository extends JpaRepository<MatchPlayerStat, Long> {

    List<MatchPlayerStat> findByMatchIdOrderByTeamIdAscPlayerIdAscStatKeyAsc(Long matchId);
}

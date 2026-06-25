package com.example.edu.sports_predict_live.livematch.stat.repository;

import com.example.edu.sports_predict_live.livematch.stat.entity.MatchTeamStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchTeamStatRepository extends JpaRepository<MatchTeamStat, Long> {

    List<MatchTeamStat> findByMatchIdOrderByTeamIdAscStatKeyAsc(Long matchId);
}

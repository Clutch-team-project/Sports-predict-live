package com.example.edu.sports_predict_live.livematch.repository;

import com.example.edu.sports_predict_live.livematch.entity.MatchTeamStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchTeamStatRepository extends JpaRepository<MatchTeamStat, Long> {

    List<MatchTeamStat> findByMatchIdOrderByTeamIdAscStatKeyAsc(Long matchId);
}

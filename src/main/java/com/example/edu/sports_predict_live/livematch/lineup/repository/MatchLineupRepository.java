package com.example.edu.sports_predict_live.livematch.lineup.repository;

import com.example.edu.sports_predict_live.livematch.lineup.entity.MatchLineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {

    List<MatchLineup> findByMatchId(Long matchId);

    List<MatchLineup> findByMatchIdIn(Collection<Long> matchIds);

    List<MatchLineup> findByMatchIdOrderByTeamIdAscStarterDescOrderNumAscMatchLineupIdAsc(Long matchId);

    void deleteByMatchId(Long matchId);
}

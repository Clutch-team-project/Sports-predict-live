package com.example.edu.sports_predict_live.livematch.repository;

import com.example.edu.sports_predict_live.livematch.entity.MatchLineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, Long> {

    List<MatchLineup> findByMatchIdOrderByTeamIdAscStarterDescOrderNumAscMatchLineupIdAsc(Long matchId);
}
package com.example.edu.sports_predict_live.team.repository;

import com.example.edu.sports_predict_live.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}

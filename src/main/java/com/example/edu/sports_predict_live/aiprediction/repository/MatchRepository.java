package com.example.edu.sports_predict_live.repository;

import com.example.edu.sports_predict_live.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository
        extends JpaRepository<MatchEntity, Long> {
}
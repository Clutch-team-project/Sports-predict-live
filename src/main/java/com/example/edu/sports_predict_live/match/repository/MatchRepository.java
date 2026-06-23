package com.example.edu.sports_predict_live.match.repository;

import com.example.edu.sports_predict_live.match.entity.Match;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        JOIN FETCH m.sport s
        WHERE m.matchId = :matchId
    """)
    Optional<Match> findByIdWithTeams(@Param("matchId") Long matchId);

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        JOIN FETCH m.sport s
        WHERE (:sportId IS NULL OR s.sportId = :sportId)
          AND (:date IS NULL OR FUNCTION('DATE', m.scheduledAt) = :date)
          AND (:status IS NULL OR m.status = :status)
        ORDER BY m.scheduledAt ASC
    """)
    List<Match> findGames(
            @Param("sportId") Long sportId,
            @Param("date") LocalDate date,
            @Param("status") String status
    );

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        JOIN FETCH m.sport s
        WHERE s.code = :sportCode
          AND FUNCTION('DATE', m.scheduledAt) = :date
        ORDER BY m.scheduledAt ASC
    """)
    List<Match> findBySportCodeAndDate(
            @Param("sportCode") String sportCode,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        JOIN FETCH m.sport s
        WHERE s.code = :sportCode
        ORDER BY m.scheduledAt DESC
    """)
    List<Match> findBySportCodeOrderByScheduledAtDesc(@Param("sportCode") String sportCode);

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        JOIN FETCH m.sport s
        WHERE s.code = :sportCode
          AND YEAR(m.scheduledAt) = :year
          AND MONTH(m.scheduledAt) = :month
        ORDER BY m.scheduledAt ASC
    """)
    List<Match> findBySportCodeAndYearMonth(
            @Param("sportCode") String sportCode,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        WHERE (ht.teamId = :teamId OR at.teamId = :teamId)
          AND m.status = 'finished'
        ORDER BY m.scheduledAt DESC
    """)
    List<Match> findRecentFinishedByTeamId(@Param("teamId") Long teamId, Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        JOIN FETCH m.homeTeam ht
        JOIN FETCH m.awayTeam at
        WHERE (ht.teamId = :teamId OR at.teamId = :teamId)
          AND m.status = 'scheduled'
        ORDER BY m.scheduledAt ASC
    """)
    List<Match> findUpcomingByTeamId(@Param("teamId") Long teamId, Pageable pageable);
}

package com.example.edu.sports_predict_live.match.repository;

import com.example.edu.sports_predict_live.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    // 종목 코드 + 날짜로 경기 조회 (시간 오름차순)
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

    // 종목 코드 + 월로 경기 조회 (달력용)
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
}

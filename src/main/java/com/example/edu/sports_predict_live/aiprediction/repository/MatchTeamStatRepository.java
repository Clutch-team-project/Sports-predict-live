package com.example.edu.sports_predict_live.aiprediction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchTeamStatRepository extends JpaRepository<Object, Long> {
    // 💡 public class를 public interface로 변경했습니다!

    // 경기 팀 통계 테이블 팀원과 합치기 전까지 독자 빌드용 임시 메서드
    default Double findAverageStatByTeam(Long teamId, String season) {
        return 0.55; // 임시 더미 데이터 반환
    }

    // MatchTeamStatRepository.java
// 임시 default 메서드를 지우고 실제 쿼리 어노테이션 적용
//    @Query("SELECT AVG(s.statValue) FROM MatchTeamStatEntity s WHERE s.teamId = :teamId AND s.season = :season")
//    Double findAverageStatByTeam(@Param("teamId") Long teamId, @Param("season") String season);
}
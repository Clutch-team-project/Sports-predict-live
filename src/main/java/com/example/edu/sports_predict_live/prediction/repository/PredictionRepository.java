package com.example.edu.sports_predict_live.prediction.repository;

import com.example.edu.sports_predict_live.prediction.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUser_UserIdAndMatch_MatchId(Long userId, Long matchId);

    Optional<Prediction> findByUser_UserIdAndLolMatchId(Long userId, String lolMatchId);

    // 미정산 LOL 예측 전체 (자동 정산용) — 취소 무효화된 예측은 재조회 대상에서 제외
    List<Prediction> findBySportCodeAndIsCorrectIsNullAndActualResultIsNull(String sportCode);

    // 미정산 KBO/Soccer 예측 (자동 정산용)
    @Query("SELECT p FROM Prediction p JOIN FETCH p.match m " +
           "WHERE p.sportCode = :sportCode AND p.isCorrect IS NULL AND m.status = 'finished'")
    List<Prediction> findUnsettledDbPredictions(@Param("sportCode") String sportCode);

    // 경기가 취소되었지만 아직 무효화 처리되지 않은 KBO/Soccer 예측 (자동 정산용)
    @Query("SELECT p FROM Prediction p JOIN FETCH p.match m " +
           "WHERE p.actualResult IS NULL AND m.status = 'cancelled'")
    List<Prediction> findCancelledDbPredictions();

    // 내 예측 기록 전체 (최신순)
    List<Prediction> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 포인트 순위 — 종목 필터 or 전체
    @Query("SELECT p.user.userId, p.user.nickname, " +
           "SUM(p.pointsEarned) as totalPoints, " +
           "SUM(CASE WHEN p.isCorrect = true THEN 1L ELSE 0L END) as correctCount, " +
           "COUNT(p) as totalCount " +
           "FROM Prediction p " +
           "WHERE (:sportCode IS NULL OR p.sportCode = :sportCode) " +
           "AND p.isCorrect IS NOT NULL " +
           "GROUP BY p.user.userId, p.user.nickname " +
           "ORDER BY totalPoints DESC")
    List<Object[]> findRanking(@Param("sportCode") String sportCode);

    // 특정 날짜 미정산 LOL 예측 (날짜별 배치 정산용) — 취소 무효화된 예측은 재조회 대상에서 제외
    List<Prediction> findBySportCodeAndLolScheduledDateAndIsCorrectIsNullAndActualResultIsNull(
            String sportCode, LocalDate lolScheduledDate);

    // 홈 통계 카드용: 정산 완료된 전체 예측 수 + 적중 수
    @Query("SELECT COUNT(p), SUM(CASE WHEN p.isCorrect = true THEN 1L ELSE 0L END) " +
           "FROM Prediction p WHERE p.isCorrect IS NOT NULL")
    List<Object[]> findGlobalStats();

    // 홈 통계 카드용: 특정 유저의 전체 예측 수 + 정산 완료된 적중 수
    @Query("SELECT COUNT(p), SUM(CASE WHEN p.isCorrect = true THEN 1L ELSE 0L END) " +
           "FROM Prediction p WHERE p.user.userId = :userId")
    List<Object[]> findUserStats(@Param("userId") Long userId);
}

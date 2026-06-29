package com.example.edu.sports_predict_live.livematch.admin.demo.repository;

import com.example.edu.sports_predict_live.livematch.admin.demo.entity.DemoMatchEventSeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DemoMatchEventSeedRepository extends JpaRepository<DemoMatchEventSeed, Long> {

    Optional<DemoMatchEventSeed> findFirstByMatchIdAndPublishedFalseAndDelaySecondsLessThanEqualOrderBySeqNoAsc(
            Long matchId,
            Integer elapsedSeconds
    );

    Optional<DemoMatchEventSeed> findFirstByMatchIdAndPublishedFalseOrderBySeqNoAsc(Long matchId);

    long countByMatchId(Long matchId);

    long countByMatchIdAndPublishedTrue(Long matchId);

    long countByMatchIdAndPublishedFalse(Long matchId);

    @Query("select max(s.delaySeconds) from DemoMatchEventSeed s where s.matchId = :matchId")
    Integer findMaxDelaySecondsByMatchId(@Param("matchId") Long matchId);
}

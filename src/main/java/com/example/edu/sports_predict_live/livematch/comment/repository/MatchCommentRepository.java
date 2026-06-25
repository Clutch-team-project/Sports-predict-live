package com.example.edu.sports_predict_live.livematch.comment.repository;

import com.example.edu.sports_predict_live.livematch.comment.entity.MatchComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchCommentRepository extends JpaRepository<MatchComment, Long> {

    List<MatchComment> findByMatch_MatchIdAndDeletedFalseOrderByCreatedAtDescCommentIdDesc(Long matchId, Pageable pageable);
}

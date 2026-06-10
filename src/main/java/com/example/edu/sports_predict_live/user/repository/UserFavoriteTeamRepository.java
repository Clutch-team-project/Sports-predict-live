package com.example.edu.sports_predict_live.user.repository;

import com.example.edu.sports_predict_live.user.entity.UserFavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteTeamRepository extends JpaRepository<UserFavoriteTeam, Long> {

    // 유저의 관심 팀 목록
    @Query("SELECT uft FROM UserFavoriteTeam uft JOIN FETCH uft.team t JOIN FETCH t.sport WHERE uft.user.userId = :userId")
    List<UserFavoriteTeam> findByUserIdWithTeam(@Param("userId") Long userId);

    // 특정 관심 팀 존재 여부
    boolean existsByUser_UserIdAndTeam_TeamId(Long userId, Long teamId);

    // 특정 관심 팀 조회 (삭제용)
    Optional<UserFavoriteTeam> findByUser_UserIdAndTeam_TeamId(Long userId, Long teamId);

    // 유저가 관심 등록한 팀 ID 목록 (프론트 ★ 상태 초기화용)
    @Query("SELECT uft.team.teamId FROM UserFavoriteTeam uft WHERE uft.user.userId = :userId")
    List<Long> findTeamIdsByUserId(@Param("userId") Long userId);
}

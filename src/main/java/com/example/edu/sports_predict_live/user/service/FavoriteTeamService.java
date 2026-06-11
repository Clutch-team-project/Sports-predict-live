package com.example.edu.sports_predict_live.user.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import com.example.edu.sports_predict_live.user.dto.response.FavoriteTeamResponseDTO;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.entity.UserFavoriteTeam;
import com.example.edu.sports_predict_live.user.repository.UserFavoriteTeamRepository;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteTeamService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UserFavoriteTeamRepository favoriteTeamRepository;

    // 관심 팀 목록 조회
    @Transactional(readOnly = true)
    public List<FavoriteTeamResponseDTO> getFavorites(Long userId) {
        return favoriteTeamRepository.findByUserIdWithTeam(userId)
                .stream()
                .map(FavoriteTeamResponseDTO::new)
                .toList();
    }

    // 관심 팀 ID 목록 조회 (프론트 ★ 초기화용)
    @Transactional(readOnly = true)
    public List<Long> getFavoriteTeamIds(Long userId) {
        return favoriteTeamRepository.findTeamIdsByUserId(userId);
    }

    // 관심 팀 추가
    @Transactional
    public void addFavorite(Long userId, Long teamId) {
        if (favoriteTeamRepository.existsByUser_UserIdAndTeam_TeamId(userId, teamId)) {
            throw new CustomException(ErrorCode.ALREADY_FAVORITE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));

        favoriteTeamRepository.save(new UserFavoriteTeam(user, team));
    }

    // 관심 팀 삭제
    @Transactional
    public void removeFavorite(Long userId, Long teamId) {
        UserFavoriteTeam favorite = favoriteTeamRepository
                .findByUser_UserIdAndTeam_TeamId(userId, teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_NOT_FOUND));

        favoriteTeamRepository.delete(favorite);
    }
}

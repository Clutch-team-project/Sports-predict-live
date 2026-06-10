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

// 관심 팀 등록/해제/조회 (user_favorite_team — 유저:팀 다대다)
@Service
@RequiredArgsConstructor
public class FavoriteTeamService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UserFavoriteTeamRepository favoriteTeamRepository;

    @Transactional(readOnly = true)
    public List<FavoriteTeamResponseDTO> getFavorites(Long userId) {
        return favoriteTeamRepository.findByUserIdWithTeam(userId)
                .stream()
                .map(FavoriteTeamResponseDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getFavoriteTeamIds(Long userId) {
        return favoriteTeamRepository.findTeamIdsByUserId(userId);
    }

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

    @Transactional
    public void removeFavorite(Long userId, Long teamId) {
        UserFavoriteTeam favorite = favoriteTeamRepository
                .findByUser_UserIdAndTeam_TeamId(userId, teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.FAVORITE_NOT_FOUND));

        favoriteTeamRepository.delete(favorite);
    }
}

package com.example.edu.sports_predict_live.team.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.match.dto.response.MatchResponseDTO;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.repository.PlayerRepository;
import com.example.edu.sports_predict_live.team.dto.response.TeamDetailResponseDTO;
import com.example.edu.sports_predict_live.team.dto.response.TeamSimpleResponseDTO;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.entity.TeamSeasonStat;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import com.example.edu.sports_predict_live.team.repository.TeamSeasonStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 팀 상세 페이지 데이터 — 기본 정보 + 시즌 성적 + 선수단 + 최근/예정 경기
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamDetailService {

    private static final String CURRENT_SEASON = "2026";
    private static final int    MATCH_LIMIT    = 5;

    private final TeamRepository teamRepository;
    private final TeamSeasonStatRepository teamSeasonStatRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public TeamDetailResponseDTO getTeamDetail(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEAM_NOT_FOUND));

        TeamSeasonStat stat = teamSeasonStatRepository
                .findByTeam_TeamIdAndSeason(teamId, CURRENT_SEASON)
                .orElse(null);

        List<Player> players = playerRepository.findByTeam_TeamIdOrderByNameAsc(teamId);

        List<MatchResponseDTO> recentMatches = matchRepository
                .findRecentFinishedByTeamId(teamId, PageRequest.of(0, MATCH_LIMIT))
                .stream().map(MatchResponseDTO::new).toList();

        List<MatchResponseDTO> upcomingMatches = matchRepository
                .findUpcomingByTeamId(teamId, PageRequest.of(0, MATCH_LIMIT))
                .stream().map(MatchResponseDTO::new).toList();

        return new TeamDetailResponseDTO(team, stat, players, recentMatches, upcomingMatches);
    }

    // LOL 순위표 이름→ID 매핑용 팀 목록
    public List<TeamSimpleResponseDTO> getTeamsBySport(String sportCode) {
        return teamRepository.findBySportCode(sportCode)
                .stream()
                .map(TeamSimpleResponseDTO::new)
                .toList();
    }
}

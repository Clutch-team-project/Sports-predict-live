package com.example.edu.sports_predict_live.team.dto.response;

import com.example.edu.sports_predict_live.match.dto.response.MatchResponseDTO;
import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.team.entity.Team;
import com.example.edu.sports_predict_live.team.entity.TeamSeasonStat;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class TeamDetailResponseDTO {

    // 기본 정보
    private final Long    teamId;
    private final String  name;
    private final String  sportCode;
    private final String  sportName;
    private final String  location;
    private final Integer foundedYear;
    private final String  emblemUrl;

    // 시즌 성적 (없으면 null)
    private final SeasonStat seasonStat;

    // 소속 선수
    private final List<PlayerSimple> players;

    // 최근/예정 경기
    private final List<MatchResponseDTO> recentMatches;
    private final List<MatchResponseDTO> upcomingMatches;

    public TeamDetailResponseDTO(Team team, TeamSeasonStat stat,
                                 List<Player> playerList,
                                 List<MatchResponseDTO> recentMatches,
                                 List<MatchResponseDTO> upcomingMatches) {
        this.teamId      = team.getTeamId();
        this.name        = team.getName();
        this.sportCode   = team.getSport().getCode();
        this.sportName   = team.getSport().getName();
        this.location    = team.getLocation();
        this.foundedYear = team.getFoundedYear();
        this.emblemUrl   = team.getEmblemUrl();
        this.seasonStat  = stat != null ? new SeasonStat(stat) : null;
        this.players     = playerList.stream().map(PlayerSimple::new).toList();
        this.recentMatches   = recentMatches;
        this.upcomingMatches = upcomingMatches;
    }

    @Getter
    public static class SeasonStat {
        private final String     season;
        private final Integer    rank;
        private final int        wins;
        private final int        draws;
        private final int        losses;
        private final BigDecimal winRate;
        private final int        pointsFor;
        private final int        pointsAgainst;
        private final String     recentForm; // 예: 'WWDLW'

        public SeasonStat(TeamSeasonStat stat) {
            this.season        = stat.getSeason();
            this.rank          = stat.getRank();
            this.wins          = stat.getWins();
            this.draws         = stat.getDraws();
            this.losses        = stat.getLosses();
            this.winRate       = stat.getWinRate();
            this.pointsFor     = stat.getPointsFor();
            this.pointsAgainst = stat.getPointsAgainst();
            this.recentForm    = stat.getRecentForm();
        }
    }

    @Getter
    public static class PlayerSimple {
        private final Long    playerId;
        private final String  name;
        private final String  position;
        private final Integer jerseyNumber;
        private final String  profileImage;

        public PlayerSimple(Player player) {
            this.playerId     = player.getPlayerId();
            this.name         = player.getName();
            this.position     = player.getPosition();
            this.jerseyNumber = player.getJerseyNumber();
            this.profileImage = player.getProfileImage();
        }
    }
}

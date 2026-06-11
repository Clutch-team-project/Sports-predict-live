package com.example.edu.sports_predict_live.player.dto.response;

import com.example.edu.sports_predict_live.player.entity.Player;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStat;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatLol;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatSoccer;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Getter
public class PlayerDetailResponseDTO {

    // 기본 정보
    private final Long      playerId;
    private final String    name;
    private final String    position;
    private final Integer   jerseyNumber;
    private final LocalDate birthDate;
    private final Integer   age;
    private final String    profileImage;

    // 소속 팀
    private final Long   teamId;
    private final String teamName;
    private final String teamEmblem;
    private final String sportCode;
    private final String sportName;

    // LOL 전용 (야구/축구는 null)
    private final String riotGameName;
    private final String riotTagLine;

    // 시즌 기록 (없으면 null)
    private final String  season;
    private final Integer gamesPlayed;
    private final BaseballStat baseballStat;
    private final SoccerStat   soccerStat;
    private final LolStat      lolStat;

    public PlayerDetailResponseDTO(Player player, PlayerSeasonStat pss,
                                   PlayerSeasonStatBaseball baseball,
                                   PlayerSeasonStatSoccer soccer,
                                   PlayerSeasonStatLol lol) {
        this.playerId     = player.getPlayerId();
        this.name         = player.getName();
        this.position     = player.getPosition();
        this.jerseyNumber = player.getJerseyNumber();
        this.birthDate    = player.getBirthDate();
        this.age          = player.getBirthDate() != null
                ? Period.between(player.getBirthDate(), LocalDate.now()).getYears()
                : null;
        this.profileImage = player.getProfileImage();

        this.teamId     = player.getTeam().getTeamId();
        this.teamName   = player.getTeam().getName();
        this.teamEmblem = player.getTeam().getEmblemUrl();
        this.sportCode  = player.getTeam().getSport().getCode();
        this.sportName  = player.getTeam().getSport().getName();

        this.riotGameName = player.getRiotGameName();
        this.riotTagLine  = player.getRiotTagLine();

        this.season      = pss != null ? pss.getSeason() : null;
        this.gamesPlayed = pss != null ? pss.getGamesPlayed() : null;
        this.baseballStat = baseball != null ? new BaseballStat(baseball) : null;
        this.soccerStat   = soccer   != null ? new SoccerStat(soccer)     : null;
        this.lolStat      = lol      != null ? new LolStat(lol)           : null;
    }

    @Getter
    public static class BaseballStat {
        // 타자 (투수는 null)
        private final BigDecimal battingAvg;
        private final Integer    hits;
        private final Integer    homeRuns;
        private final Integer    rbi;
        // 투수 (타자는 null)
        private final BigDecimal era;
        private final Integer    wins;
        private final Integer    losses;
        private final Integer    strikeouts;
        private final Integer    saves;
        private final Integer    holds;

        public BaseballStat(PlayerSeasonStatBaseball stat) {
            this.battingAvg = stat.getBattingAvg();
            this.hits       = stat.getHits();
            this.homeRuns   = stat.getHomeRuns();
            this.rbi        = stat.getRbi();
            this.era        = stat.getEra();
            this.wins       = stat.getWins();
            this.losses     = stat.getLosses();
            this.strikeouts = stat.getStrikeouts();
            this.saves      = stat.getSaves();
            this.holds      = stat.getHolds();
        }
    }

    @Getter
    public static class SoccerStat {
        private final int goals;
        private final int assists;
        private final int yellowCards;
        private final int redCards;
        private final int cleanSheets;

        public SoccerStat(PlayerSeasonStatSoccer stat) {
            this.goals       = stat.getGoals();
            this.assists     = stat.getAssists();
            this.yellowCards = stat.getYellowCards();
            this.redCards    = stat.getRedCards();
            this.cleanSheets = stat.getCleanSheets();
        }
    }

    @Getter
    public static class LolStat {
        private final BigDecimal kda;
        private final BigDecimal avgKills;
        private final BigDecimal avgDeaths;
        private final BigDecimal avgAssists;
        private final BigDecimal csPerMin;
        private final BigDecimal winRate;

        public LolStat(PlayerSeasonStatLol stat) {
            this.kda        = stat.getKda();
            this.avgKills   = stat.getAvgKills();
            this.avgDeaths  = stat.getAvgDeaths();
            this.avgAssists = stat.getAvgAssists();
            this.csPerMin   = stat.getCsPerMin();
            this.winRate    = stat.getWinRate();
        }
    }
}

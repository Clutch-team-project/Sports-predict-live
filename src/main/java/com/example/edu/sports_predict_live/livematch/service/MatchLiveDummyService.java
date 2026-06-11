package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchLiveDTO;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchLiveDummyService {

    private final MatchRepository matchRepository;
    private final PlayerSeasonStatBaseballRepository baseballRecordRepository;
    private static final String CURRENT_SEASON = "2026";

    /*
     * 화면 렌더링 검증용 중계 데이터 생성.
     * 경기 기본 정보는 크롤러가 채운 match/team 데이터를 우선 사용하고,
     * 아직 DB 구조가 없는 현재 타석/댓글/예측 영역은 더미로 유지한다.
     *
     * 이 단계의 목적:
     * - /api/games/{matchId}/baseball-live 응답이 실제 경기 ID와 연결되는지 확인
     * - 최종 이벤트 타임라인은 match_event 기반 API로 전환
     */
    @Transactional(readOnly = true)
    public MatchLiveDTO getBaseballLive(Long matchId) {
        return matchRepository.findById(matchId)
                .map(this::toLiveDto)
                .orElseGet(() -> dummyLiveDto(matchId));
    }

    private MatchLiveDTO toLiveDto(Match match) {
        String currentInning = resolveCurrentInning(match.getStatus());
        String homeShortName = shortName(match.getHomeTeam().getName());
        String awayShortName = shortName(match.getAwayTeam().getName());
        MatchLiveDTO.CurrentPlayer currentBatter = resolveCurrentBatter(match);
        MatchLiveDTO.CurrentPlayer currentPitcher = resolveCurrentPitcher(match);

        return new MatchLiveDTO(
                match.getMatchId(),
                match.getSport().getCode(),
                match.getStatus(),
                currentInning,
                new MatchLiveDTO.TeamScore(
                        match.getHomeTeam().getTeamId(),
                        match.getHomeTeam().getName(),
                        homeShortName,
                        logoText(match.getHomeTeam().getName()),
                        "home"
                ),
                new MatchLiveDTO.TeamScore(
                        match.getAwayTeam().getTeamId(),
                        match.getAwayTeam().getName(),
                        awayShortName,
                        logoText(match.getAwayTeam().getName()),
                        "away"
                ),
                new MatchLiveDTO.Score(match.getHomeScore(), match.getAwayScore()),
                currentBatter,
                currentPitcher,
                new MatchLiveDTO.Count(0, 0, 0),
                new MatchLiveDTO.Runners(false, false, false),
                List.of(
                        new MatchLiveDTO.InningScore(awayShortName, List.of("-", "-", "-", "-", "-", "-", "-", "-", "-"), match.getAwayScore(), 0, 0, 0),
                        new MatchLiveDTO.InningScore(homeShortName, List.of("-", "-", "-", "-", "-", "-", "-", "-", "-"), match.getHomeScore(), 0, 0, 0)
                ),
                List.of(
                        new MatchLiveDTO.LiveEvent("match_loaded", 0, currentInning, "크롤링 경기 데이터 연결", "match_id " + match.getMatchId() + " · " + match.getAwayTeam().getName() + " vs " + match.getHomeTeam().getName()),
                        new MatchLiveDTO.LiveEvent("venue", 0, "구장", "경기 장소", match.getVenue() != null ? match.getVenue() : "구장 정보 없음"),
                        new MatchLiveDTO.LiveEvent("scheduled_at", 0, "경기 시간", "예정 시각", match.getScheduledAt().toString()),
                        new MatchLiveDTO.LiveEvent("batter_record", 0, "크롤링 타자 기록", currentBatter.name(), currentBatter.description()),
                        new MatchLiveDTO.LiveEvent("pitcher_record", 0, "크롤링 투수 기록", currentPitcher.name(), currentPitcher.description())
                ),
                List.of(
                        new MatchLiveDTO.CommentPreview("system", "크롤러가 저장한 match/team/score 데이터로 중계 상단을 렌더링합니다.", "방금"),
                        new MatchLiveDTO.CommentPreview("system", "크롤링된 야구 시즌 기록에서 양 팀 대표 타자·투수를 표시합니다.", "방금"),
                        new MatchLiveDTO.CommentPreview("system", "이닝별 득점·볼카운트·주자 상황은 아직 저장 컬럼이 없어 다음 단계에서 match_event로 분리합니다.", "방금")
                ),
                new MatchLiveDTO.PredictionPreview(match.getHomeTeam().getName(), match.getAwayTeam().getName(), 50, 50, 0, 0, true),
                new MatchLiveDTO.ViewerState(false, false, false)
        );
    }

    private MatchLiveDTO dummyLiveDto(Long matchId) {
        return new MatchLiveDTO(
                matchId,
                "baseball",
                "LIVE",
                "9회말",
                new MatchLiveDTO.TeamScore(101L, "코딩왕 자이언츠", "코딩왕", "💻", "away"),
                new MatchLiveDTO.TeamScore(102L, "버그제로 이글스", "버그제로", "🛡️", "home"),
                new MatchLiveDTO.Score(5, 4),
                new MatchLiveDTO.CurrentPlayer(2001L, "나천재", "버그제로 이글스", "7번 타자 · 1루수 · 타율 .333"),
                new MatchLiveDTO.CurrentPlayer(3001L, "고칠게", "코딩왕 자이언츠", "8.2이닝 115구 5실점"),
                new MatchLiveDTO.Count(3, 2, 2),
                new MatchLiveDTO.Runners(true, false, true),
                List.of(
                        new MatchLiveDTO.InningScore("코딩왕", List.of("1", "0", "2", "0", "0", "1", "0", "1", "0"), 5, 12, 0, 4),
                        new MatchLiveDTO.InningScore("버그제로", List.of("0", "2", "0", "1", "0", "0", "1", "0", "-"), 4, 9, 1, 3)
                ),
                List.of(
                        new MatchLiveDTO.LiveEvent("at_bat_start", 0, "9회말", "나천재 타석 시작", "버그제로 이글스 공격, 2사 1·3루. 역전 찬스입니다."),
                        new MatchLiveDTO.LiveEvent("ball", 1, "볼카운트 1 - 0", "볼", "148km/h 직구"),
                        new MatchLiveDTO.LiveEvent("strike", 2, "볼카운트 1 - 1", "스트라이크", "132km/h 슬라이더"),
                        new MatchLiveDTO.LiveEvent("foul", 3, "볼카운트 1 - 2", "파울", "145km/h 직구"),
                        new MatchLiveDTO.LiveEvent("ball", 4, "볼카운트 2 - 2", "볼", "128km/h 체인지업"),
                        new MatchLiveDTO.LiveEvent("ball", 5, "볼카운트 3 - 2", "볼", "147km/h 직구"),
                        new MatchLiveDTO.LiveEvent("foul", 6, "볼카운트 3 - 2", "파울", "142km/h 커터"),
                        new MatchLiveDTO.LiveEvent("hit", 7, "2사 1·3루", "타격", "좌전 안타!"),
                        new MatchLiveDTO.LiveEvent("video_review", 0, "21:55", "홈 세이프/아웃 판독", "3루 주자 이재현의 홈 태그 상황에 대한 비디오 판독이 진행 중입니다."),
                        new MatchLiveDTO.LiveEvent("result", 0, "판독 결과", "세이프 판정 유지", "3루 주자 이재현 : 홈인 (5-5 동점)"),
                        new MatchLiveDTO.LiveEvent("result", 0, "주자 상황", "1사 1,2루", "1루 주자 김지찬 : 2루까지 진루")
                ),
                List.of(
                        new MatchLiveDTO.CommentPreview("자바장인", "와 드디어 동점!! 9회말 2사에서 이걸 해내네", "1분 전"),
                        new MatchLiveDTO.CommentPreview("스프링초보", "비디오 판독 개떨린다 제발 세이프!!", "30초 전"),
                        new MatchLiveDTO.CommentPreview("버그제로팬", "나천재! 나천재! 나천재!", "방금")
                ),
                new MatchLiveDTO.PredictionPreview("버그제로", "코딩왕", 65, 35, 2450, 1120, false),
                new MatchLiveDTO.ViewerState(true, true, false)
        );
    }

    private String resolveCurrentInning(String status) {
        return switch (status) {
            case "finished" -> "경기 종료";
            case "in_progress" -> "경기 진행 중";
            case "paused" -> "경기 중단";
            case "cancelled" -> "경기 취소";
            default -> "경기 예정";
        };
    }

    private String shortName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "-";
        }
        return teamName.length() <= 3 ? teamName : teamName.substring(0, 3);
    }

    private String logoText(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            return "-";
        }
        return teamName.substring(0, 1);
    }

    private MatchLiveDTO.CurrentPlayer resolveCurrentBatter(Match match) {
        Optional<PlayerSeasonStatBaseball> record = baseballRecordRepository
                .findHittersBySportAndSeason("baseball", CURRENT_SEASON)
                .stream()
                .filter(stat -> stat.getPlayerSeasonStat().getPlayer().getTeam().getTeamId().equals(match.getHomeTeam().getTeamId()))
                .findFirst();

        return record.map(stat -> new MatchLiveDTO.CurrentPlayer(
                stat.getPlayerSeasonStat().getPlayer().getPlayerId(),
                stat.getPlayerSeasonStat().getPlayer().getName(),
                match.getHomeTeam().getName(),
                "크롤링 타자 기록 · 타율 " + valueOrDash(stat.getBattingAvg())
                        + " · 안타 " + valueOrZero(stat.getHits())
                        + " · 홈런 " + valueOrZero(stat.getHomeRuns())
                        + " · 타점 " + valueOrZero(stat.getRbi())
        )).orElseGet(() -> new MatchLiveDTO.CurrentPlayer(
                0L,
                "타자 기록 없음",
                match.getHomeTeam().getName(),
                "크롤링된 홈팀 타자 기록이 아직 없습니다."
        ));
    }

    private MatchLiveDTO.CurrentPlayer resolveCurrentPitcher(Match match) {
        Optional<PlayerSeasonStatBaseball> record = baseballRecordRepository
                .findPitchersBySportAndSeason("baseball", CURRENT_SEASON)
                .stream()
                .filter(stat -> stat.getPlayerSeasonStat().getPlayer().getTeam().getTeamId().equals(match.getAwayTeam().getTeamId()))
                .findFirst();

        return record.map(stat -> new MatchLiveDTO.CurrentPlayer(
                stat.getPlayerSeasonStat().getPlayer().getPlayerId(),
                stat.getPlayerSeasonStat().getPlayer().getName(),
                match.getAwayTeam().getName(),
                "크롤링 투수 기록 · ERA " + valueOrDash(stat.getEra())
                        + " · 승 " + valueOrZero(stat.getWins())
                        + " · 패 " + valueOrZero(stat.getLosses())
                        + " · 삼진 " + valueOrZero(stat.getStrikeouts())
                        + " · 세이브 " + valueOrZero(stat.getSaves())
                        + " · 홀드 " + valueOrZero(stat.getHolds())
        )).orElseGet(() -> new MatchLiveDTO.CurrentPlayer(
                0L,
                "투수 기록 없음",
                match.getAwayTeam().getName(),
                "크롤링된 원정팀 투수 기록이 아직 없습니다."
        ));
    }

    private String valueOrDash(Object value) {
        return value != null ? value.toString() : "-";
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }
}

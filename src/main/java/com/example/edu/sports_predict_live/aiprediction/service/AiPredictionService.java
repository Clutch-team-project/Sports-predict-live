package com.example.edu.sports_predict_live.aiprediction.service;

import com.example.edu.sports_predict_live.aiprediction.client.LolApiClient;
import com.example.edu.sports_predict_live.aiprediction.client.OpenAiClient;
import com.example.edu.sports_predict_live.aiprediction.dto.AiPredictionResponseDTO;
import com.example.edu.sports_predict_live.aiprediction.entity.AiPrediction;
import com.example.edu.sports_predict_live.aiprediction.repository.AiPredictionRepository;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatBaseball;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatLol;
import com.example.edu.sports_predict_live.player.entity.PlayerSeasonStatSoccer;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatBaseballRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatLolRepository;
import com.example.edu.sports_predict_live.player.repository.PlayerSeasonStatSoccerRepository;
import com.example.edu.sports_predict_live.team.entity.TeamSeasonStat;
import com.example.edu.sports_predict_live.team.repository.TeamSeasonStatRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPredictionService {

    private final MatchRepository matchRepository;
    private final AiPredictionRepository aiPredictionRepository;
    private final TeamSeasonStatRepository teamSeasonStatRepository;
    private final PlayerSeasonStatBaseballRepository pitcherRepository;
    private final PlayerSeasonStatSoccerRepository soccerPlayerRepository;
    private final PlayerSeasonStatLolRepository lolPlayerRepository;
    private final OpenAiClient openAiClient;
    private final LolApiClient lolApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ══════════════════════════════════════════════
    //  야구 예측
    // ══════════════════════════════════════════════

    @Transactional
    public AiPredictionResponseDTO predictBaseball(Long matchId) {
        Optional<AiPrediction> cached = aiPredictionRepository.findByMatch_MatchId(matchId);
        if (cached.isPresent() && isCacheValidBaseball(cached.get())) {
            return toResponseDTO(cached.get());
        }
        if (cached.isPresent()) {
            aiPredictionRepository.delete(cached.get());
            aiPredictionRepository.flush();
        }

        Match match = findMatch(matchId);
        String season = resolveSeason(match.getSeason());

        TeamSeasonStat homeStat = teamSeasonStatRepository.findByTeam_TeamIdAndSeason(match.getHomeTeam().getTeamId(), season).orElse(null);
        TeamSeasonStat awayStat = teamSeasonStatRepository.findByTeam_TeamIdAndSeason(match.getAwayTeam().getTeamId(), season).orElse(null);
        PlayerSeasonStatBaseball homePitcher = findPitcherStat(match.getStartingPitcherHome(), season);
        PlayerSeasonStatBaseball awayPitcher = findPitcherStat(match.getStartingPitcherAway(), season);
        List<Match> h2h = matchRepository.findHeadToHead(match.getHomeTeam().getTeamId(), match.getAwayTeam().getTeamId(), season);

        String prompt = buildBaseballPrompt(match, season, homeStat, awayStat, homePitcher, awayPitcher, h2h);
        JsonNode ai = openAiClient.requestPrediction(prompt, false); // 야구: 무승부 없음

        BigDecimal[] probs = normalize(
                toBD(ai, "home_win_prob"), toBD(ai, "draw_prob"), toBD(ai, "away_win_prob"));

        String basis = buildBaseballBasis(homeStat, awayStat, homePitcher, awayPitcher,
                match.getStartingPitcherHome(), match.getStartingPitcherAway(), h2h, match.getVenue());

        aiPredictionRepository.save(AiPrediction.builder()
                .match(match).sportCode("baseball")
                .homeWinProb(probs[0]).drawProb(probs[1]).awayWinProb(probs[2])
                .basis(basis)
                .reasoning(ai.path("reasoning").asText(""))
                .keyFactors(toJsonArray(ai.path("key_factors")))
                .build());

        return buildResponse(probs, ai);
    }

    // ══════════════════════════════════════════════
    //  축구 예측
    // ══════════════════════════════════════════════

    @Transactional
    public AiPredictionResponseDTO predictSoccer(Long matchId) {
        Optional<AiPrediction> cached = aiPredictionRepository.findByMatch_MatchId(matchId);
        if (cached.isPresent()) return toResponseDTO(cached.get());

        Match match = findMatch(matchId);
        String season = resolveSeason(match.getSeason());

        TeamSeasonStat homeStat = teamSeasonStatRepository.findByTeam_TeamIdAndSeason(match.getHomeTeam().getTeamId(), season).orElse(null);
        TeamSeasonStat awayStat = teamSeasonStatRepository.findByTeam_TeamIdAndSeason(match.getAwayTeam().getTeamId(), season).orElse(null);

        List<PlayerSeasonStatSoccer> homePlayers = soccerPlayerRepository.findTopByTeamAndSeason(match.getHomeTeam().getTeamId(), season);
        List<PlayerSeasonStatSoccer> awayPlayers = soccerPlayerRepository.findTopByTeamAndSeason(match.getAwayTeam().getTeamId(), season);

        List<Match> h2h = matchRepository.findHeadToHead(match.getHomeTeam().getTeamId(), match.getAwayTeam().getTeamId(), season);

        String prompt = buildSoccerPrompt(match, season, homeStat, awayStat, homePlayers, awayPlayers, h2h);
        JsonNode ai = openAiClient.requestPrediction(prompt, true); // 축구: 무승부 있음

        BigDecimal[] probs = normalize(
                toBD(ai, "home_win_prob"), toBD(ai, "draw_prob"), toBD(ai, "away_win_prob"));

        aiPredictionRepository.save(AiPrediction.builder()
                .match(match).sportCode("soccer")
                .homeWinProb(probs[0]).drawProb(probs[1]).awayWinProb(probs[2])
                .basis("{}")
                .reasoning(ai.path("reasoning").asText(""))
                .keyFactors(toJsonArray(ai.path("key_factors")))
                .build());

        return buildResponse(probs, ai);
    }

    // ══════════════════════════════════════════════
    //  LoL 예측
    // ══════════════════════════════════════════════

    @Transactional
    public AiPredictionResponseDTO predictLol(String lolMatchId, String homeTeamName, String awayTeamName, String season) {
        Optional<AiPrediction> cached = aiPredictionRepository.findByLolMatchId(lolMatchId);
        if (cached.isPresent()) return toResponseDTO(cached.get());

        Map<String, Map<String, Object>> standings = lolApiClient.getStandings();
        int[] h2h = lolApiClient.getHeadToHead(homeTeamName, awayTeamName);

        List<PlayerSeasonStatLol> homePlayers = lolPlayerRepository.findByTeamNameAndSeason(homeTeamName, season);
        List<PlayerSeasonStatLol> awayPlayers = lolPlayerRepository.findByTeamNameAndSeason(awayTeamName, season);

        String prompt = buildLolPrompt(homeTeamName, awayTeamName, season, standings, homePlayers, awayPlayers, h2h);
        JsonNode ai = openAiClient.requestPrediction(prompt, false); // LoL: 무승부 없음

        BigDecimal[] probs = normalize(
                toBD(ai, "home_win_prob"), toBD(ai, "draw_prob"), toBD(ai, "away_win_prob"));

        aiPredictionRepository.save(AiPrediction.builder()
                .lolMatchId(lolMatchId).sportCode("lol")
                .homeWinProb(probs[0]).drawProb(probs[1]).awayWinProb(probs[2])
                .basis("{}")
                .reasoning(ai.path("reasoning").asText(""))
                .keyFactors(toJsonArray(ai.path("key_factors")))
                .build());

        return buildResponse(probs, ai);
    }

    // ══════════════════════════════════════════════
    //  프롬프트 빌더
    // ══════════════════════════════════════════════

    private String buildBaseballPrompt(Match match, String season,
                                       TeamSeasonStat homeStat, TeamSeasonStat awayStat,
                                       PlayerSeasonStatBaseball homePitcher, PlayerSeasonStatBaseball awayPitcher,
                                       List<Match> h2hMatches) {
        StringBuilder sb = new StringBuilder("=== KBO 야구 경기 예측 데이터 ===\n\n");

        sb.append("[홈팀] ").append(match.getHomeTeam().getName()).append("\n");
        appendTeamStat(sb, homeStat);
        sb.append("  선발 투수: ").append(formatPitcher(match.getStartingPitcherHome(), homePitcher)).append("\n\n");

        sb.append("[원정팀] ").append(match.getAwayTeam().getName()).append("\n");
        appendTeamStat(sb, awayStat);
        sb.append("  선발 투수: ").append(formatPitcher(match.getStartingPitcherAway(), awayPitcher)).append("\n\n");

        appendH2H(sb, match.getHomeTeam().getTeamId(), match.getHomeTeam().getName(),
                match.getAwayTeam().getName(), h2hMatches, season);

        sb.append("[경기장] ").append(match.getVenue() != null ? match.getVenue() : "미정").append(" (홈 이점)\n\n");
        sb.append("위 데이터로 이 경기 결과를 예측해 주세요. 야구는 무승부가 없으므로 draw_prob=0.0000으로 고정하세요.");
        return sb.toString();
    }

    private String buildSoccerPrompt(Match match, String season,
                                     TeamSeasonStat homeStat, TeamSeasonStat awayStat,
                                     List<PlayerSeasonStatSoccer> homePlayers,
                                     List<PlayerSeasonStatSoccer> awayPlayers,
                                     List<Match> h2hMatches) {
        StringBuilder sb = new StringBuilder("=== K리그 축구 경기 예측 데이터 ===\n\n");

        sb.append("[홈팀] ").append(match.getHomeTeam().getName()).append("\n");
        appendSoccerTeamStat(sb, homeStat);
        appendSoccerTopPlayers(sb, homePlayers);
        sb.append("\n");

        sb.append("[원정팀] ").append(match.getAwayTeam().getName()).append("\n");
        appendSoccerTeamStat(sb, awayStat);
        appendSoccerTopPlayers(sb, awayPlayers);
        sb.append("\n");

        appendH2H(sb, match.getHomeTeam().getTeamId(), match.getHomeTeam().getName(),
                match.getAwayTeam().getName(), h2hMatches, season);

        sb.append("[경기장] ").append(match.getVenue() != null ? match.getVenue() : "미정").append(" (홈 이점)\n\n");
        sb.append("위 데이터로 이 경기 결과를 예측해 주세요. 축구는 무승부가 있으므로 draw_prob를 자유롭게 설정하세요.");
        return sb.toString();
    }

    private String buildLolPrompt(String homeTeamName, String awayTeamName, String season,
                                  Map<String, Map<String, Object>> standings,
                                  List<PlayerSeasonStatLol> homePlayers,
                                  List<PlayerSeasonStatLol> awayPlayers,
                                  int[] h2h) {
        StringBuilder sb = new StringBuilder("=== LCK LoL 경기 예측 데이터 ===\n\n");

        sb.append("[홈팀] ").append(homeTeamName).append("\n");
        appendLolTeamStat(sb, standings.get(homeTeamName));
        appendLolTopPlayers(sb, homePlayers);
        sb.append("\n");

        sb.append("[원정팀] ").append(awayTeamName).append("\n");
        appendLolTeamStat(sb, standings.get(awayTeamName));
        appendLolTopPlayers(sb, awayPlayers);
        sb.append("\n");

        sb.append("[상대 전적 (").append(season).append("시즌)]\n");
        sb.append(homeTeamName).append(" ").append(h2h[0]).append("승 / ");
        sb.append(awayTeamName).append(" ").append(h2h[1]).append("승\n\n");

        sb.append("위 데이터로 이 경기 결과를 예측해 주세요. LoL은 무승부가 없으므로 draw_prob=0.0000으로 고정하세요.");
        return sb.toString();
    }

    // ── 팀/선수 정보 포맷 ───────────────────────────────────────────

    private void appendTeamStat(StringBuilder sb, TeamSeasonStat stat) {
        if (stat == null) { sb.append("  시즌 성적: 데이터 없음\n"); return; }
        sb.append("  시즌 성적: ").append(stat.getWins()).append("승 ")
          .append(stat.getDraws()).append("무 ").append(stat.getLosses()).append("패");
        if (stat.getWinRate() != null) sb.append(" (승률 ").append(stat.getWinRate()).append(")");
        if (stat.getRank() != null)    sb.append(" / ").append(stat.getRank()).append("위");
        sb.append("\n  최근 5경기: ").append(stat.getRecentForm() != null ? stat.getRecentForm() : "데이터 없음").append("\n");
    }

    private void appendSoccerTeamStat(StringBuilder sb, TeamSeasonStat stat) {
        if (stat == null) { sb.append("  시즌 성적: 데이터 없음\n"); return; }
        sb.append("  시즌 성적: ").append(stat.getWins()).append("승 ")
          .append(stat.getDraws()).append("무 ").append(stat.getLosses()).append("패");
        if (stat.getRank() != null) sb.append(" / ").append(stat.getRank()).append("위");
        sb.append("\n  득점: ").append(stat.getPointsFor())
          .append(" / 실점: ").append(stat.getPointsAgainst())
          .append("\n  최근 5경기: ").append(stat.getRecentForm() != null ? stat.getRecentForm() : "없음").append("\n");
    }

    private void appendSoccerTopPlayers(StringBuilder sb, List<PlayerSeasonStatSoccer> players) {
        if (players == null || players.isEmpty()) return;
        sb.append("  주요 선수 (득점 순):\n");
        players.stream().limit(3).forEach(p -> {
            String name = p.getPlayerSeasonStat().getPlayer().getName();
            sb.append("    - ").append(name)
              .append(" | 득점 ").append(p.getGoals())
              .append(" / 도움 ").append(p.getAssists()).append("\n");
        });
    }

    private void appendLolTeamStat(StringBuilder sb, Map<String, Object> standing) {
        if (standing == null) { sb.append("  순위 데이터 없음\n"); return; }
        sb.append("  순위: ").append(standing.get("rank")).append("위")
          .append(" / ").append(standing.get("wins")).append("승 ").append(standing.get("losses")).append("패")
          .append(String.format(" (승률 %.1f%%)\n", (double) standing.get("winRate") * 100));
    }

    private void appendLolTopPlayers(StringBuilder sb, List<PlayerSeasonStatLol> players) {
        if (players == null || players.isEmpty()) return;
        sb.append("  주요 선수 (KDA 순):\n");
        players.stream().limit(3).forEach(p -> {
            String name = p.getPlayerSeasonStat().getPlayer().getName();
            String pos  = p.getPlayerSeasonStat().getPlayer().getPosition();
            sb.append("    - [").append(pos != null ? pos : "?").append("] ").append(name)
              .append(" | KDA ").append(p.getKda() != null ? p.getKda() : "N/A")
              .append(String.format(" / 승률 %.1f%%\n", p.getWinRate() != null ? p.getWinRate().doubleValue() * 100 : 0.0));
        });
    }

    private void appendH2H(StringBuilder sb, Long homeTeamId, String homeName,
                           String awayName, List<Match> h2hMatches, String season) {
        long hw = 0, aw = 0, d = 0;
        for (Match m : h2hMatches) {
            if (m.getHomeScore() > m.getAwayScore()) {
                if (m.getHomeTeam().getTeamId().equals(homeTeamId)) hw++; else aw++;
            } else if (m.getAwayScore() > m.getHomeScore()) {
                if (m.getAwayTeam().getTeamId().equals(homeTeamId)) aw++; else hw++;
            } else { d++; }
        }
        sb.append("[상대 전적 (").append(season).append("시즌)]\n")
          .append(homeName).append(" ").append(hw).append("승 / ")
          .append(awayName).append(" ").append(aw).append("승 / 무 ").append(d).append("\n\n");
    }

    private String formatPitcher(String name, PlayerSeasonStatBaseball stat) {
        if (name == null || name.isBlank()) return "미정";
        if (stat == null) return name + " (성적 없음)";
        return String.format("%s — ERA %.2f, %d승 %d패, %d삼진", name,
                stat.getEra() != null ? stat.getEra().doubleValue() : 0.0,
                stat.getWins() != null ? stat.getWins() : 0,
                stat.getLosses() != null ? stat.getLosses() : 0,
                stat.getStrikeouts() != null ? stat.getStrikeouts() : 0);
    }

    // ── 공통 유틸 ───────────────────────────────────────────────────

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다. matchId=" + matchId));
    }

    private String resolveSeason(String season) {
        return season != null ? season : String.valueOf(java.time.LocalDate.now().getYear());
    }

    private PlayerSeasonStatBaseball findPitcherStat(String name, String season) {
        if (name == null || name.isBlank()) return null;
        return pitcherRepository.findPitcherByNameAndSeason(name, season).orElse(null);
    }

    private BigDecimal toBD(JsonNode node, String field) {
        return BigDecimal.valueOf(node.path(field).asDouble(0.0)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal[] normalize(BigDecimal home, BigDecimal draw, BigDecimal away) {
        BigDecimal sum = home.add(draw).add(away);
        if (sum.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal[]{
                bd(0.5), bd(0.0), bd(0.5)
            };
        }
        BigDecimal nh = home.divide(sum, 4, RoundingMode.HALF_UP);
        BigDecimal nd = draw.divide(sum, 4, RoundingMode.HALF_UP);
        BigDecimal na = BigDecimal.ONE.subtract(nh).subtract(nd).setScale(4, RoundingMode.HALF_UP);
        return new BigDecimal[]{nh, nd, na};
    }

    private BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP);
    }

    private AiPredictionResponseDTO buildResponse(BigDecimal[] probs, JsonNode ai) {
        double hp = probs[0].multiply(BigDecimal.valueOf(100)).doubleValue();
        double dp = probs[1].multiply(BigDecimal.valueOf(100)).doubleValue();
        double ap = probs[2].multiply(BigDecimal.valueOf(100)).doubleValue();
        return AiPredictionResponseDTO.builder()
                .homeWinProb(hp).drawProb(dp).awayWinProb(ap)
                .reasoning(ai.path("reasoning").asText(""))
                .keyFactors(toStringList(ai.path("key_factors")))
                .predictedResult(hp >= ap ? "HOME_WIN" : "AWAY_WIN")
                .build();
    }

    private AiPredictionResponseDTO toResponseDTO(AiPrediction pred) {
        double hp = pred.getHomeWinProb().multiply(BigDecimal.valueOf(100)).doubleValue();
        double dp = pred.getDrawProb().multiply(BigDecimal.valueOf(100)).doubleValue();
        double ap = pred.getAwayWinProb().multiply(BigDecimal.valueOf(100)).doubleValue();
        List<String> factors = parseJsonArray(pred.getKeyFactors());
        return AiPredictionResponseDTO.builder()
                .homeWinProb(hp).drawProb(dp).awayWinProb(ap)
                .reasoning(pred.getReasoning() != null ? pred.getReasoning() : "")
                .keyFactors(factors)
                .predictedResult(hp >= ap ? "HOME_WIN" : "AWAY_WIN")
                .build();
    }

    private boolean isCacheValidBaseball(AiPrediction cached) {
        try {
            Match match = cached.getMatch();
            boolean matchHasPitcher = (match.getStartingPitcherHome() != null && !match.getStartingPitcherHome().isBlank())
                    || (match.getStartingPitcherAway() != null && !match.getStartingPitcherAway().isBlank());
            if (!matchHasPitcher) return true;
            if (cached.getBasis() == null) return false;
            JsonNode basis = objectMapper.readTree(cached.getBasis());
            String ch = basis.path("home_pitcher").path("name").asText("");
            String ca = basis.path("away_pitcher").path("name").asText("");
            if (match.getStartingPitcherHome() != null && !match.getStartingPitcherHome().isBlank() && ch.isBlank()) return false;
            if (match.getStartingPitcherAway() != null && !match.getStartingPitcherAway().isBlank() && ca.isBlank()) return false;
            return true;
        } catch (Exception e) { return false; }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) node.forEach(n -> list.add(n.asText()));
        return list;
    }

    private String toJsonArray(JsonNode node) {
        try {
            return node.isArray() ? objectMapper.writeValueAsString(node) : "[]";
        } catch (Exception e) { return "[]"; }
    }

    private List<String> parseJsonArray(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            JsonNode node = objectMapper.readTree(json);
            return toStringList(node);
        } catch (Exception e) { return List.of(); }
    }

    private String buildBaseballBasis(TeamSeasonStat hs, TeamSeasonStat as,
                                      PlayerSeasonStatBaseball hp, PlayerSeasonStatBaseball ap,
                                      String hpName, String apName,
                                      List<Match> h2h, String venue) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.set("home_season",   buildTeamNode(hs));
            root.set("away_season",   buildTeamNode(as));
            root.set("home_pitcher",  buildPitcherNode(hpName, hp));
            root.set("away_pitcher",  buildPitcherNode(apName, ap));
            long hw=0, aw=0, d=0;
            for (Match m : h2h) {
                if (m.getHomeScore() > m.getAwayScore()) hw++;
                else if (m.getAwayScore() > m.getHomeScore()) aw++;
                else d++;
            }
            ObjectNode h2hNode = objectMapper.createObjectNode();
            h2hNode.put("home_wins", hw); h2hNode.put("away_wins", aw); h2hNode.put("draws", d);
            root.set("head_to_head", h2hNode);
            root.put("venue", venue != null ? venue : "");
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) { return "{}"; }
    }

    private ObjectNode buildTeamNode(TeamSeasonStat stat) {
        ObjectNode n = objectMapper.createObjectNode();
        if (stat == null) return n;
        n.put("wins", stat.getWins()); n.put("draws", stat.getDraws()); n.put("losses", stat.getLosses());
        n.put("win_rate", stat.getWinRate() != null ? stat.getWinRate().doubleValue() : 0.0);
        n.put("rank", stat.getRank() != null ? stat.getRank() : 0);
        n.put("recent_form", stat.getRecentForm() != null ? stat.getRecentForm() : "");
        return n;
    }

    private ObjectNode buildPitcherNode(String name, PlayerSeasonStatBaseball stat) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("name", name != null ? name : "");
        if (stat == null) return n;
        n.put("era", stat.getEra() != null ? stat.getEra().doubleValue() : 0.0);
        n.put("wins", stat.getWins() != null ? stat.getWins() : 0);
        n.put("losses", stat.getLosses() != null ? stat.getLosses() : 0);
        n.put("strikeouts", stat.getStrikeouts() != null ? stat.getStrikeouts() : 0);
        return n;
    }
}

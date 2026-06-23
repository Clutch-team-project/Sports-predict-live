package com.example.edu.sports_predict_live.aiprediction.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class LolApiClient {

    private static final String LOL_API_KEY   = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";
    private static final String LOL_API_BASE  = "https://esports-api.lolesports.com/persisted/gw";
    private static final String LCK_LEAGUE_ID = "98767991310872058";

    private final WebClient webClient = WebClient.builder()
            .baseUrl(LOL_API_BASE)
            .defaultHeader("x-api-key", LOL_API_KEY)
            .build();

    /**
     * LCK 현재 스플릿 팀 순위 반환
     * 반환 형태: Map<teamName, Map{wins, losses, winRate, rank}>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getStandings() {
        Map<String, Object> response = webClient.get()
                .uri(uri -> uri.path("/getStandings")
                        .queryParam("hl", "ko-KR")
                        .queryParam("leagueId", LCK_LEAGUE_ID)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(new HashMap<>())
                .block();

        Map<String, Map<String, Object>> result = new HashMap<>();
        if (response == null || response.isEmpty()) return result;

        try {
            List<Map<String, Object>> stages = (List<Map<String, Object>>)
                    ((Map<?, ?>) ((Map<?, ?>) response.get("data")).get("standings")).get("stages");
            if (stages == null || stages.isEmpty()) return result;

            // 가장 최근 stage 기준
            Map<String, Object> latestStage = stages.get(stages.size() - 1);
            List<Map<String, Object>> sections = (List<Map<String, Object>>) latestStage.get("sections");
            if (sections == null || sections.isEmpty()) return result;

            List<Map<String, Object>> rankings = (List<Map<String, Object>>) sections.get(0).get("rankings");
            if (rankings == null) return result;

            int rank = 1;
            for (Map<String, Object> ranking : rankings) {
                List<Map<String, Object>> teams = (List<Map<String, Object>>) ranking.get("teams");
                if (teams == null) continue;
                for (Map<String, Object> team : teams) {
                    String teamName = (String) team.get("name");
                    Map<String, Object> record = (Map<String, Object>) team.get("record");
                    if (teamName == null || record == null) continue;

                    int wins   = (int) record.getOrDefault("wins",   0);
                    int losses = (int) record.getOrDefault("losses", 0);
                    double winRate = (wins + losses) > 0 ? (double) wins / (wins + losses) : 0.0;

                    Map<String, Object> info = new HashMap<>();
                    info.put("wins",    wins);
                    info.put("losses",  losses);
                    info.put("winRate", winRate);
                    info.put("rank",    rank);
                    result.put(teamName, info);
                    rank++;
                }
            }
        } catch (Exception e) {
            log.warn("LoL 순위 파싱 실패", e);
        }
        return result;
    }

    /**
     * LCK 팀명 → 로고 이미지 URL 맵 반환 (팀 엠블럼 초기화용)
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getTeamImages() {
        Map<String, Object> response = webClient.get()
                .uri(uri -> uri.path("/getStandings")
                        .queryParam("hl", "ko-KR")
                        .queryParam("leagueId", LCK_LEAGUE_ID)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .onErrorReturn(new HashMap<>())
                .block();

        Map<String, String> result = new HashMap<>();
        if (response == null || response.isEmpty()) return result;

        try {
            List<Map<String, Object>> stages = (List<Map<String, Object>>)
                    ((Map<?, ?>) ((Map<?, ?>) response.get("data")).get("standings")).get("stages");
            if (stages == null || stages.isEmpty()) return result;

            List<Map<String, Object>> rankings = (List<Map<String, Object>>)
                    ((Map<?, ?>) ((List<?>) stages.get(stages.size() - 1).get("sections")).get(0)).get("rankings");
            if (rankings == null) return result;

            for (Map<String, Object> ranking : rankings) {
                List<Map<String, Object>> teams = (List<Map<String, Object>>) ranking.get("teams");
                if (teams == null) continue;
                for (Map<String, Object> team : teams) {
                    String name  = (String) team.get("name");
                    String image = (String) team.get("image");
                    if (name != null && image != null && !image.isBlank()) {
                        result.put(name, image);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LCK 팀 이미지 파싱 실패", e);
        }
        return result;
    }

    /**
     * 두 팀의 현재 시즌 맞대결 기록 집계
     * 반환: int[] { homeWins, awayWins }
     */
    @SuppressWarnings("unchecked")
    public int[] getHeadToHead(String homeTeamName, String awayTeamName) {
        int[] record = {0, 0}; // [homeWins, awayWins]
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uri -> uri.path("/getSchedule")
                            .queryParam("hl", "ko-KR")
                            .queryParam("leagueId", LCK_LEAGUE_ID)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .onErrorReturn(new HashMap<>())
                    .block();

            if (response == null || response.isEmpty()) return record;

            List<Map<String, Object>> events = (List<Map<String, Object>>)
                    ((Map<?, ?>) ((Map<?, ?>) response.get("data")).get("schedule")).get("events");
            if (events == null) return record;

            for (Map<String, Object> event : events) {
                if (!"match".equals(event.get("type"))) continue;
                if (!"completed".equals(event.get("state"))) continue;

                Map<String, Object> match = (Map<String, Object>) event.get("match");
                if (match == null) continue;

                List<Map<String, Object>> teams = (List<Map<String, Object>>) match.get("teams");
                if (teams == null || teams.size() < 2) continue;

                String name0 = (String) teams.get(0).get("name");
                String name1 = (String) teams.get(1).get("name");

                boolean isH2H = (homeTeamName.equals(name0) && awayTeamName.equals(name1))
                             || (homeTeamName.equals(name1) && awayTeamName.equals(name0));
                if (!isH2H) continue;

                Map<String, Object> r0 = (Map<String, Object>) teams.get(0).get("result");
                Map<String, Object> r1 = (Map<String, Object>) teams.get(1).get("result");
                if (r0 == null || r1 == null) continue;

                int wins0 = (int) r0.getOrDefault("gameWins", 0);
                int wins1 = (int) r1.getOrDefault("gameWins", 0);
                String winner = wins0 > wins1 ? name0 : name1;

                if (winner.equals(homeTeamName)) record[0]++;
                else record[1]++;
            }
        } catch (Exception e) {
            log.warn("LoL 상대전적 파싱 실패", e);
        }
        return record;
    }
}

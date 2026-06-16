package com.example.edu.sports_predict_live.team.service;

import com.example.edu.sports_predict_live.team.dto.response.StandingsResponseDTO;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import com.example.edu.sports_predict_live.team.repository.TeamSeasonStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 팀 순위 조회 — KBO/K리그는 DB(team_season_stat), LOL은 lolesports API 실시간 호출
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingsService {

    private final TeamSeasonStatRepository teamSeasonStatRepository;
    private final TeamRepository teamRepository;

    private static final String CURRENT_SEASON  = "2026";
    private static final String LOL_API_KEY      = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";
    private static final String LOL_API_BASE     = "https://esports-api.lolesports.com/persisted/gw";
    private static final String LCK_LEAGUE_ID    = "98767991310872058";

    private final WebClient webClient = WebClient.builder()
            .baseUrl(LOL_API_BASE)
            .defaultHeader("x-api-key", LOL_API_KEY)
            .build();

    public List<StandingsResponseDTO> getStandings(String sport) {
        if ("lol".equals(sport)) {
            return getLolStandingsFromApi();
        }
        return teamSeasonStatRepository
                .findBySportCodeAndSeason(sport, CURRENT_SEASON)
                .stream()
                .map(StandingsResponseDTO::new)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<StandingsResponseDTO> getLolStandingsFromApi() {
        // 현재 날짜 기준으로 진행 중인 토너먼트 ID 조회
        String tournamentId = getCurrentTournamentId();
        if (tournamentId == null) return List.of();

        Map<String, Object> response = webClient.get()
                .uri(uri -> uri.path("/getStandings")
                        .queryParam("hl", "ko-KR")
                        .queryParam("tournamentId", tournamentId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return List.of();

        List<Map<String, Object>> standingsList =
                (List<Map<String, Object>>) ((Map<?, ?>) response.get("data")).get("standings");
        if (standingsList == null) return List.of();

        // DB LOL 팀명 → teamId 매핑 (상세 페이지/관심 팀 연동용)
        // DB team.name은 lolesports API의 code 값("T1", "GEN" 등)으로 저장됨
        Map<String, Long> teamIdByCode = new java.util.HashMap<>();
        teamRepository.findBySportCode("lol").forEach(t ->
                teamIdByCode.put(t.getName().toLowerCase(), t.getTeamId()));

        // 순위 API에는 최근 폼이 없어 일정 API의 완료 경기에서 팀별 최근 5경기 계산
        Map<String, String> recentFormByCode = getLolRecentForms();

        List<StandingsResponseDTO> results = new ArrayList<>();
        for (Map<String, Object> tournament : standingsList) {
            List<Map<String, Object>> stages =
                    (List<Map<String, Object>>) tournament.get("stages");
            if (stages == null) continue;
            for (Map<String, Object> stage : stages) {
                // 정규시즌(regular_season)만 사용
                if (!"regular_season".equals(stage.get("slug"))) continue;
                List<Map<String, Object>> sections =
                        (List<Map<String, Object>>) stage.get("sections");
                if (sections == null) continue;
                for (Map<String, Object> section : sections) {
                    List<Map<String, Object>> rankings =
                            (List<Map<String, Object>>) section.get("rankings");
                    if (rankings == null) continue;
                    for (Map<String, Object> ranking : rankings) {
                        int ordinal = (int) ranking.get("ordinal");
                        List<Map<String, Object>> teams =
                                (List<Map<String, Object>>) ranking.get("teams");
                        if (teams == null) continue;
                        for (Map<String, Object> team : teams) {
                            Map<String, Object> record = (Map<String, Object>) team.get("record");
                            int wins   = record != null ? (int) record.get("wins")   : 0;
                            int losses = record != null ? (int) record.get("losses") : 0;
                            String teamName = (String) team.get("name");
                            String teamCode = (String) team.get("code");
                            Long teamId = teamCode != null
                                    ? teamIdByCode.get(teamCode.toLowerCase())
                                    : null;
                            results.add(new StandingsResponseDTO(
                                    teamId,
                                    ordinal,
                                    teamName,
                                    (String) team.getOrDefault("image", ""),
                                    wins,
                                    losses,
                                    teamCode != null ? recentFormByCode.get(teamCode) : null
                            ));
                        }
                    }
                }
            }
        }
        return results;
    }

    // 일정 API의 완료 경기를 시간순으로 훑어 팀 코드별 최근 5경기 W/L 문자열 생성 (왼쪽이 과거)
    @SuppressWarnings("unchecked")
    private Map<String, String> getLolRecentForms() {
        Map<String, String> forms = new java.util.HashMap<>();
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uri -> uri.path("/getSchedule")
                            .queryParam("hl", "ko-KR")
                            .queryParam("leagueId", LCK_LEAGUE_ID)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return forms;

            List<Map<String, Object>> events = (List<Map<String, Object>>)
                    ((Map<?, ?>) ((Map<?, ?>) response.get("data")).get("schedule")).get("events");
            if (events == null) return forms;

            // 이벤트는 시간 오름차순으로 제공됨 — 순서대로 누적 후 마지막 5개만 사용
            Map<String, StringBuilder> acc = new java.util.HashMap<>();
            for (Map<String, Object> event : events) {
                if (!"completed".equals(event.get("state"))) continue;
                if (!"match".equals(event.get("type"))) continue;

                Map<String, Object> match = (Map<String, Object>) event.get("match");
                if (match == null) continue;
                List<Map<String, Object>> teams = (List<Map<String, Object>>) match.get("teams");
                if (teams == null || teams.size() < 2) continue;

                for (Map<String, Object> team : teams) {
                    String code = (String) team.get("code");
                    Map<String, Object> result = (Map<String, Object>) team.get("result");
                    if (code == null || result == null) continue;
                    String outcome = (String) result.get("outcome");
                    if (!"win".equals(outcome) && !"loss".equals(outcome)) continue;
                    acc.computeIfAbsent(code, k -> new StringBuilder())
                            .append("win".equals(outcome) ? 'W' : 'L');
                }
            }
            acc.forEach((code, sb) -> {
                String all = sb.toString();
                forms.put(code, all.length() > 5 ? all.substring(all.length() - 5) : all);
            });
        } catch (Exception e) {
            // 폼 계산 실패 시 순위 자체는 정상 표시되도록 빈 맵 반환
        }
        return forms;
    }

    @SuppressWarnings("unchecked")
    private String getCurrentTournamentId() {
        Map<String, Object> response = webClient.get()
                .uri(uri -> uri.path("/getTournamentsForLeague")
                        .queryParam("hl", "ko-KR")
                        .queryParam("leagueId", LCK_LEAGUE_ID)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) return null;

        List<Map<String, Object>> leagues =
                (List<Map<String, Object>>) ((Map<?, ?>) response.get("data")).get("leagues");
        if (leagues == null || leagues.isEmpty()) return null;

        List<Map<String, Object>> tournaments =
                (List<Map<String, Object>>) leagues.get(0).get("tournaments");
        if (tournaments == null) return null;

        LocalDate today = LocalDate.now();
        Map<String, Object> mostRecentlyEnded = null;
        LocalDate mostRecentEnd = null;
        for (Map<String, Object> t : tournaments) {
            LocalDate start = LocalDate.parse((String) t.get("startDate"));
            LocalDate end   = LocalDate.parse((String) t.get("endDate"));
            if (!today.isBefore(start) && !today.isAfter(end)) {
                return (String) t.get("id");
            }
            // 진행 중인 토너먼트가 없을 경우를 대비해 종료일이 오늘 이전이면서 가장 최근인 토너먼트를 추적
            if (today.isAfter(end) && (mostRecentEnd == null || end.isAfter(mostRecentEnd))) {
                mostRecentEnd = end;
                mostRecentlyEnded = t;
            }
        }
        // 진행 중인 토너먼트 없으면 가장 최근 종료된 것 사용
        return mostRecentlyEnded != null ? (String) mostRecentlyEnded.get("id") : null;
    }
}

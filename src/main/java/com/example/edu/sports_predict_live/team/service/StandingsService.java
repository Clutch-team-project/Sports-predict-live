package com.example.edu.sports_predict_live.team.service;

import com.example.edu.sports_predict_live.team.dto.response.StandingsResponseDTO;
import com.example.edu.sports_predict_live.team.repository.TeamSeasonStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StandingsService {

    private final TeamSeasonStatRepository teamSeasonStatRepository;

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
                            results.add(new StandingsResponseDTO(
                                    ordinal,
                                    (String) team.get("name"),
                                    (String) team.getOrDefault("image", ""),
                                    wins,
                                    losses
                            ));
                        }
                    }
                }
            }
        }
        return results;
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
        for (Map<String, Object> t : tournaments) {
            LocalDate start = LocalDate.parse((String) t.get("startDate"));
            LocalDate end   = LocalDate.parse((String) t.get("endDate"));
            if (!today.isBefore(start) && !today.isAfter(end)) {
                return (String) t.get("id");
            }
        }
        // 진행 중인 토너먼트 없으면 가장 최근 종료된 것 사용
        return (String) tournaments.get(0).get("id");
    }
}

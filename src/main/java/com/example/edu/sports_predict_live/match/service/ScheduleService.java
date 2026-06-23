package com.example.edu.sports_predict_live.match.service;


import com.example.edu.sports_predict_live.match.dto.response.MatchResponseDTO;
import com.example.edu.sports_predict_live.match.entity.Match;
import com.example.edu.sports_predict_live.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 경기 일정 조회 — KBO/K리그는 DB(match 테이블), LOL은 lolesports API 실시간 호출
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final MatchRepository matchRepository;

    // lolesports API 설정
    private static final String LOL_API_KEY      = "0TvQnueqKa5mxJntVWt0w4LpLfEkrV1Ta8rQBb9Z";
    private static final String LOL_API_BASE     = "https://esports-api.lolesports.com/persisted/gw";
    private static final String LCK_LEAGUE_ID    = "98767991310872058";

    private final WebClient webClient = WebClient.builder()
            .baseUrl(LOL_API_BASE)
            .defaultHeader("x-api-key", LOL_API_KEY)
            .build();

    public List<MatchResponseDTO> getSchedule(String sport, String dateStr) {
        LocalDate date = (dateStr != null)
                ? LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                : LocalDate.now();

        return matchRepository.findBySportCodeAndDate(sport, date)
                .stream()
                .map(match -> toScheduleResponse(sport, match))
                .toList();
    }

    public List<MatchResponseDTO> getScheduleAll(String sport) {
        return matchRepository.findBySportCodeOrderByScheduledAtDesc(sport)
                .stream()
                .map(match -> toScheduleResponse(sport, match))
                .toList();
    }

    private MatchResponseDTO toScheduleResponse(String sport, Match match) {
        String status = match.getStatus();

        if ("in_progress".equals(status)) {
            status = "live";
        }

        return new MatchResponseDTO(
                match,
                match.getHomeScore(),
                match.getAwayScore(),
                status
        );
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getLolSchedule(String dateStr) {
        LocalDate targetDate = (dateStr != null)
                ? LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
                : LocalDate.now();

        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/getSchedule")
                        .queryParam("hl", "ko-KR")
                        .queryParam("leagueId", LCK_LEAGUE_ID)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> results = new ArrayList<>();
        if (response == null) return results;

        List<Map<String, Object>> events = (List<Map<String, Object>>)
                ((Map<?, ?>) ((Map<?, ?>) response.get("data")).get("schedule")).get("events");

        if (events == null) return results;

        for (Map<String, Object> event : events) {
            String startTime = (String) event.get("startTime"); // UTC, 예: "2026-04-10T10:00:00Z"
            if (startTime == null) continue;

            // UTC → KST(+9) 날짜 변환 후 조회 날짜만 필터링
            int hour = Integer.parseInt(startTime.substring(11, 13));
            LocalDate kstDate = (hour + 9 >= 24)
                    ? LocalDate.parse(startTime.substring(0, 10)).plusDays(1)
                    : LocalDate.parse(startTime.substring(0, 10));

            if (!kstDate.equals(targetDate)) continue;

            if (!"match".equals(event.get("type"))) continue;

            Map<String, Object> match = (Map<String, Object>) event.get("match");
            if (match == null) continue;

            List<Map<String, Object>> teams = (List<Map<String, Object>>) match.get("teams");
            if (teams == null || teams.size() < 2) continue;

            Map<String, Object> team1 = teams.get(0);
            Map<String, Object> team2 = teams.get(1);
            Map<String, Object> result1 = (Map<String, Object>) team1.get("result");
            Map<String, Object> result2 = (Map<String, Object>) team2.get("result");

            int kstHour = (hour + 9) % 24;
            int kstMinute = Integer.parseInt(startTime.substring(14, 16));
            String kstTime = String.format("%02d:%02d", kstHour, kstMinute);

            String state = (String) event.get("state");
            String status = switch (state != null ? state : "") {
                case "completed" -> "finished";
                case "inProgress" -> "in_progress";
                case "unstarted"  -> "scheduled";
                default -> "scheduled";
            };

            Map<String, Object> matchMap = new java.util.HashMap<>();
            matchMap.put("matchId",       match.get("id"));
            matchMap.put("homeTeamName",  team2.get("name"));
            matchMap.put("awayTeamName",  team1.get("name"));
            matchMap.put("homeTeamCode",  team2.get("code"));
            matchMap.put("awayTeamCode",  team1.get("code"));
            matchMap.put("homeTeamImage", team2.getOrDefault("image", ""));
            matchMap.put("awayTeamImage", team1.getOrDefault("image", ""));
            matchMap.put("homeWins",      result2 != null ? result2.getOrDefault("gameWins", 0) : 0);
            matchMap.put("awayWins",      result1 != null ? result1.getOrDefault("gameWins", 0) : 0);
            matchMap.put("scheduledAt",   kstTime);
            matchMap.put("status",        status);
            matchMap.put("blockName",     event.getOrDefault("blockName", ""));
            results.add(matchMap);
        }

        return results;
    }
}

package com.example.edu.sports_predict_live.global.config;

import com.example.edu.sports_predict_live.aiprediction.client.LolApiClient;
import com.example.edu.sports_predict_live.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamEmblemInitializer implements ApplicationRunner {

    private final TeamRepository teamRepository;
    private final LolApiClient lolApiClient;

    private static final Map<String, String> KBO_EMBLEMS = new HashMap<>();
    private static final Map<String, String> KLEAGUE_EMBLEMS = new HashMap<>();

    static {
        // KBO — 팀 이름 기준 (team_id는 시드 순서에 따라 달라질 수 있음)
        KBO_EMBLEMS.put("LG",   "/images/teams/KBO/LG.svg");
        KBO_EMBLEMS.put("KT",   "/images/teams/KBO/KT.svg");
        KBO_EMBLEMS.put("삼성", "/images/teams/KBO/SANSUNG.svg");
        KBO_EMBLEMS.put("KIA",  "/images/teams/KBO/KIA.svg");
        KBO_EMBLEMS.put("한화", "/images/teams/KBO/HANWHA.svg");
        KBO_EMBLEMS.put("두산", "/images/teams/KBO/DOOSAN.svg");
        KBO_EMBLEMS.put("NC",   "/images/teams/KBO/NC.svg");
        KBO_EMBLEMS.put("SSG",  "/images/teams/KBO/SSG.svg");
        KBO_EMBLEMS.put("롯데", "/images/teams/KBO/LOTTE.svg");
        KBO_EMBLEMS.put("키움", "/images/teams/KBO/KIWOOM.svg");

        // K-League — 팀 이름 기준 (team_id는 시드 순서에 따라 달라질 수 있음)
        KLEAGUE_EMBLEMS.put("서울", "/images/teams/KLeague/FCSEOUL.svg");
        KLEAGUE_EMBLEMS.put("울산", "/images/teams/KLeague/ULSANHD.svg");
        KLEAGUE_EMBLEMS.put("전북", "/images/teams/KLeague/JEONBUK.svg");
        KLEAGUE_EMBLEMS.put("강원", "/images/teams/KLeague/GANGWONFC.svg");
        KLEAGUE_EMBLEMS.put("포항", "/images/teams/KLeague/STEELERS.svg");
        KLEAGUE_EMBLEMS.put("인천", "/images/teams/KLeague/IUFC.svg");
        KLEAGUE_EMBLEMS.put("안양", "/images/teams/KLeague/FCANYANG.svg");
        KLEAGUE_EMBLEMS.put("제주", "/images/teams/KLeague/JEJU.svg");
        KLEAGUE_EMBLEMS.put("부천", "/images/teams/KLeague/BUCHEON.svg");
        KLEAGUE_EMBLEMS.put("대전", "/images/teams/KLeague/DHFC.svg");
        KLEAGUE_EMBLEMS.put("김천", "/images/teams/KLeague/GIMCHEON.svg");
        KLEAGUE_EMBLEMS.put("광주", "/images/teams/KLeague/GWANGJU.svg");
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // KBO
        KBO_EMBLEMS.forEach((name, url) -> teamRepository.updateEmblemUrlByName(name, url));

        // K-League
        KLEAGUE_EMBLEMS.forEach((name, url) -> teamRepository.updateEmblemUrlByName(name, url));

        // LCK — LoL Esports API에서 이미지 URL 추출
        try {
            Map<String, String> lolImages = lolApiClient.getTeamImages();
            lolImages.forEach((name, url) -> teamRepository.updateEmblemUrlByName(name, url));
            log.info("팀 엠블럼 초기화 완료 — LCK {}팀 이미지 적용", lolImages.size());
        } catch (Exception e) {
            log.warn("LCK 팀 이미지 초기화 실패 (API 오류) — 건너뜀", e);
        }
    }
}

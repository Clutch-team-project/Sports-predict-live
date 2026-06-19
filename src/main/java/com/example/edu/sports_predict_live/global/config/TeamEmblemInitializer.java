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

    private static final Map<Long, String> KBO_EMBLEMS = new HashMap<>();
    private static final Map<Long, String> KLEAGUE_EMBLEMS = new HashMap<>();

    static {
        // KBO
        KBO_EMBLEMS.put(1L,  "/images/teams/KBO/LG.svg");
        KBO_EMBLEMS.put(2L,  "/images/teams/KBO/KT.svg");
        KBO_EMBLEMS.put(3L,  "/images/teams/KBO/SANSUNG.svg");
        KBO_EMBLEMS.put(4L,  "/images/teams/KBO/KIA.svg");
        KBO_EMBLEMS.put(5L,  "/images/teams/KBO/HANWHA.svg");
        KBO_EMBLEMS.put(6L,  "/images/teams/KBO/DOOSAN.svg");
        KBO_EMBLEMS.put(7L,  "/images/teams/KBO/NC.svg");
        KBO_EMBLEMS.put(8L,  "/images/teams/KBO/SSG.svg");
        KBO_EMBLEMS.put(9L,  "/images/teams/KBO/LOTTE.svg");
        KBO_EMBLEMS.put(10L, "/images/teams/KBO/KIWOOM.svg");

        // K-League
        KLEAGUE_EMBLEMS.put(61L, "/images/teams/KLeague/FCSEOUL.svg");
        KLEAGUE_EMBLEMS.put(62L, "/images/teams/KLeague/ULSANHD.svg");
        KLEAGUE_EMBLEMS.put(63L, "/images/teams/KLeague/JEONBUK.svg");
        KLEAGUE_EMBLEMS.put(64L, "/images/teams/KLeague/GANGWONFC.svg");
        KLEAGUE_EMBLEMS.put(65L, "/images/teams/KLeague/STEELERS.svg");
        KLEAGUE_EMBLEMS.put(66L, "/images/teams/KLeague/IUFC.svg");
        KLEAGUE_EMBLEMS.put(67L, "/images/teams/KLeague/FCANYANG.svg");
        KLEAGUE_EMBLEMS.put(68L, "/images/teams/KLeague/JEJU.svg");
        KLEAGUE_EMBLEMS.put(69L, "/images/teams/KLeague/BUCHEON.svg");
        KLEAGUE_EMBLEMS.put(70L, "/images/teams/KLeague/DHFC.svg");
        KLEAGUE_EMBLEMS.put(71L, "/images/teams/KLeague/GIMCHEON.svg");
        KLEAGUE_EMBLEMS.put(72L, "/images/teams/KLeague/GWANGJU.svg");
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // KBO
        KBO_EMBLEMS.forEach((id, url) -> teamRepository.updateEmblemUrl(id, url));

        // K-League
        KLEAGUE_EMBLEMS.forEach((id, url) -> teamRepository.updateEmblemUrl(id, url));

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

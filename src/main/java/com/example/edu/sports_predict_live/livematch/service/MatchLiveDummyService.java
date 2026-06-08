package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchLiveDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchLiveDummyService {

    /*
     * 화면 렌더링 검증용 더미 데이터 생성.
     * DB를 조회하지 않고, 중계 화면에 필요할 법한 데이터 모양만 만들어서 돌려준다.
     *
     * 이 단계의 목적:
     * - API URL이 정상 동작하는지 확인
     * - 프론트가 나중에 받을 JSON 구조를 먼저 고정
     *
     * 다음 단계에서 HTML fetch를 붙이면 이 DTO 값들이 화면에 반영된다.
     */
    public MatchLiveDTO getBaseballLive(Long matchId) {
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
}

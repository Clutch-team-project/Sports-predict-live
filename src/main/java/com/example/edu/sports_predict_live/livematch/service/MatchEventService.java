package com.example.edu.sports_predict_live.livematch.service;

import com.example.edu.sports_predict_live.livematch.dto.MatchEventDTO;
import com.example.edu.sports_predict_live.livematch.repository.MatchEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchEventService {

    private final MatchEventRepository matchEventRepository;

    /*
     * 경기 이벤트 조회 비즈니스 흐름.
     * Controller가 넘긴 gameId를 DB의 match_id로 보고 match_event를 조회한다.
     *
     * Repository 결과는 Entity이므로, API 응답으로 바로 내보내지 않고 MatchEventDTO로 변환한다.
     * Entity는 DB 테이블 구조이고, DTO는 프론트/클라이언트에 보여줄 응답 구조다.
     */
    public List<MatchEventDTO> getMatchEvents(Long gameId) {
        return matchEventRepository.findByMatchIdOrderByEventTimeAscMatchEventIdAsc(gameId).stream()
                .map(MatchEventDTO::from)
                .toList();
    }
}

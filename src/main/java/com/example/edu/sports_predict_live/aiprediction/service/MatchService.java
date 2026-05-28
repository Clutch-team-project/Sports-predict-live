package com.example.edu.sports_predict_live.service;

import com.example.edu.sports_predict_live.entity.MatchEntity;
import com.example.edu.sports_predict_live.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

    // 전체 경기 조회
    public List<MatchEntity> getAllMatches() {

        return matchRepository.findAll();
    }

    // 경기 단건 조회
    public MatchEntity getMatch(Long matchId) {

        return matchRepository.findById(matchId)
                .orElse(null);
    }
} //실제 db 연결 시 /match/list, /match/detail 같은 API 바로 붙일 수 있게 기본 구조 미리 만드는 단계

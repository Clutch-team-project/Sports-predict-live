package com.example.edu.sports_predict_live.board.service;

import com.example.edu.sports_predict_live.board.entity.Board;
import com.example.edu.sports_predict_live.board.repository.BoardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ai 필터링 텍스트를 검사하여 부적절하면 true, 정상적이면 false를 반환, ai 오류시 false를 반환하여 일단 통과
@Slf4j
@Service
@RequiredArgsConstructor
public class AiModerationService {
    private final BoardRepository boardRepository;
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper;

    public boolean isBadContent(String text) {
        String prompt = "너는 스포츠 커뮤니티의 클린봇이야. 다음 텍스트를 분석해서 심한 욕설, 타 팀 비하, 혐오 표현, 분란 조장이 포함되어 있다면 오직 'true'를, 정상적인 글이라면 오직 'false'만 대답해. 부연 설명은 절대 하지마.\n\n분석할 텍스트: " + text;
        try {
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            String requestUrl = apiUrl + "?key=" + apiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, requestEntity, String.class);

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String aiResponse = rootNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText().trim().toLowerCase();

            log.info("🤖 AI 검사 결과 (원본 텍스트 일부: {}...): {}", text.substring(0, Math.min(text.length(), 10)), aiResponse);

            return aiResponse.contains("true");

        } catch (Exception e) {
            log.error("AI 검사 중 서버 통신 오류 발생", e);
            return false;
        }
    }

    @Async
    @Transactional
    public void checkAndBlindAsync(Long boardId, String textToAnalyze) {
        log.info("백그라운드에서 [{}]번 게시글 ai 필터링", boardId);

        boolean isBad = isBadContent(textToAnalyze);

        if(isBad) {
            log.info("[{}]번 게시글에서 부적절한 내용 감지,자동 블라인드 처리 진행", boardId);
            Board board = boardRepository.findById(boardId).orElse(null);
            if(board != null) {
                board.changeBlind(true);
                boardRepository.save(board);
            }
        }
    }
}
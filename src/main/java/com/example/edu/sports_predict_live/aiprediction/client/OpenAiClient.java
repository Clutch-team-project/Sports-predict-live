package com.example.edu.sports_predict_live.aiprediction.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL      = "gpt-4o-mini";

    private static final String SYSTEM_BASE =
            "당신은 스포츠 전문 분석가입니다. 제공된 데이터를 기반으로 경기 결과를 예측하고, " +
            "반드시 아래 JSON 형식으로만 응답하세요.\n" +
            "{\n" +
            "  \"home_win_prob\": 0.587,\n" +
            "  \"draw_prob\": 0.000,\n" +
            "  \"away_win_prob\": 0.413,\n" +
            "  \"reasoning\": \"예측 근거 설명 2~3문장\",\n" +
            "  \"key_factors\": [\"핵심 근거1\", \"핵심 근거2\", \"핵심 근거3\"]\n" +
            "}\n" +
            "규칙: home_win_prob + draw_prob + away_win_prob = 1.0 이어야 합니다.";

    private static final String NO_DRAW_RULE =
            " 이 종목은 무승부가 없으므로 draw_prob는 반드시 0.0000으로 설정하세요.";

    @Value("${openai.api-key}")
    private String apiKey;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param allowDraw true이면 무승부 확률 자유 설정 (축구), false이면 0 고정 (야구·LoL)
     */
    public JsonNode requestPrediction(String userPrompt, boolean allowDraw) {
        String systemPrompt = allowDraw ? SYSTEM_BASE : SYSTEM_BASE + NO_DRAW_RULE;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", MODEL);
        body.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));

        ArrayNode messages = objectMapper.createArrayNode();
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);
        body.set("messages", messages);

        String raw = webClient.post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root    = objectMapper.readTree(raw);
            String   content = root.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readTree(content);
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 실패: {}", raw, e);
            throw new RuntimeException("AI 예측 응답 파싱에 실패했습니다.");
        }
    }
}

package com.example.edu.sports_predict_live.news.service;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import com.example.edu.sports_predict_live.news.repository.NewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class NewsApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NewsRepository newsRepository;

    private final String CLIENT_ID = "01WqnP5WnzxUPdjgIBMi";
    private final String CLIENT_SECRET = "sOCkEPhUh8";

    public JsonNode searchSportsNews(String keyword) {

        String url =
                "https://openapi.naver.com/v1/search/news.json?query="
                        + keyword
                        + "&display=10&sort=date";

        HttpHeaders headers = new HttpHeaders();

        headers.set("X-Naver-Client-Id", CLIENT_ID);
        headers.set("X-Naver-Client-Secret", CLIENT_SECRET);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        String.class
                );

        try {

            JsonNode jsonNode =
                    objectMapper.readTree(response.getBody());

            JsonNode items = jsonNode.get("items");

            for (JsonNode item : items) {

                String title = item.get("title")
                        .asText()
                        .replaceAll("<[^>]*>", "");

                String description = item.get("description")
                        .asText()
                        .replaceAll("<[^>]*>", "");

                String link = item.get("originallink").asText();

                String pubDate = item.get("pubDate").asText();

                // 중복 뉴스 방지
                boolean exists =
                        newsRepository.existsByNewsUrl(link);

                if (exists) {
                    continue;
                }

                // 뉴스 엔티티 생성
                NewsEntity news = new NewsEntity();

                news.setCategory(keyword);

                news.setTitle(title);

                news.setSummary(description);

                news.setNewsUrl(link);

                news.setSource("NAVER");

                // 저장
                newsRepository.save(news);

                System.out.println("뉴스 저장 완료 : " + title);
            }

            return jsonNode;

        } catch (Exception e) {

            throw new RuntimeException("뉴스 JSON 파싱 실패");
        }
    }
}
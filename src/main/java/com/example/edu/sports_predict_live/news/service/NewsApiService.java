package com.example.edu.sports_predict_live.news.service;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import com.example.edu.sports_predict_live.news.repository.NewsRepository;
import com.example.edu.sports_predict_live.news.repository.NewsScrapRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Service
@RequiredArgsConstructor
public class NewsApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NewsRepository newsRepository;
    private final NewsScrapRepository newsScrapRepository;

    private final String CLIENT_ID = "01WqnP5WnzxUPdjgIBMi";
    private final String CLIENT_SECRET = "sOCkEPhUh8";

    public JsonNode searchSportsNews(String keyword, String sort, Long userId) {

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

                String thumbnailUrl = extractThumbnailUrl(link);

                Long sportId = 1L;

// 축구
                if (keyword.contains("축구")) {

                    sportId = 1L;
                }

// 야구
                else if (keyword.contains("야구")) {

                    sportId = 2L;
                }

// LOL
                else if (
                        keyword.contains("LOL")
                                || keyword.contains("롤")
                ) {

                    sportId = 3L;
                }

                String originLink = item.get("originallink").asText();

                String[] parts = originLink.split("/");

                String source =
                        parts.length > 2
                                ? parts[2]
                                : "NAVER";

                LocalDateTime publishedAt =
                        ZonedDateTime.parse(
                                pubDate,
                                DateTimeFormatter.RFC_1123_DATE_TIME
                        ).toLocalDateTime();

                // 중복 뉴스 방지
                NewsEntity savedNews;

                boolean exists = newsRepository.existsByNewsUrl(link);

                if (exists) {
                    savedNews = newsRepository.findTopByNewsUrl(link);
                } else {
                    NewsEntity news = new NewsEntity();

                    news.setCategory(keyword);
                    news.setTitle(title);
                    news.setSummary(description);
                    news.setNewsUrl(link);
                    news.setSource("NAVER");

                    savedNews = newsRepository.save(news);

                    System.out.println("뉴스 저장 완료 : " + savedNews.getTitle());
                }

                ((ObjectNode) item).put("newsId", savedNews.getNewsId());
                ((ObjectNode) item).put("thumbnailUrl", thumbnailUrl);

                Long scrapCount = newsScrapRepository.countByNewsId(savedNews.getNewsId());
                ((ObjectNode) item).put("scrapCount", scrapCount);

                // 뉴스 엔티티 생성
                NewsEntity news = new NewsEntity();

                news.setSportId(sportId);

                news.setCategory(keyword);

                String team = "기타";

                if (
                        title.contains("손흥민")
                                || title.contains("토트넘")
                ) {
                    team = "토트넘";
                }
                else if (
                        title.contains("이강인")
                                || title.contains("PSG")
                ) {
                    team = "PSG";
                }
                else if (
                        title.contains("페이커")
                                || title.contains("T1")
                ) {
                    team = "T1";
                }

                news.setTitle(title);

                news.setSummary(description);

                news.setNewsUrl(link);

                news.setSource(source);

                news.setPublishedAt(publishedAt);

                news.setCreatedAt(LocalDateTime.now());

                news.setTeam(team);

                news.setThumbnailUrl(thumbnailUrl);

                // 저장
                savedNews = newsRepository.save(news);

                System.out.println("뉴스 저장 완료 : " + savedNews.getTitle());
                System.out.println("저장된 뉴스 ID : " + savedNews.getNewsId());
            }

            return jsonNode;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("뉴스 JSON 파싱 실패", e);
        }
    }

    private String extractThumbnailUrl(String articleUrl) {
        try {
            Document document = Jsoup.connect(articleUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(3000)
                    .get();

            String imageUrl = document
                    .select("meta[property=og:image]")
                    .attr("content");

            if (imageUrl == null || imageUrl.isBlank()) {
                return null;
            }

            return imageUrl;

        } catch (Exception e) {
            return null;
        }
    }
}
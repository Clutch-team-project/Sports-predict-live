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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    public JsonNode searchSportsNews(String keyword, String sort, Long userId, int page, String team){


        int start = (page - 1) * 10 + 1;

        String url =
                "https://openapi.naver.com/v1/search/news.json?query="
                        + keyword
                        + "&display=10"
                        + "&start=" + start
                        + "&sort=date";

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

            // 1) 팀 필터를 통과한 항목만 수집
            List<ObjectNode> targets = new ArrayList<>();
            for (JsonNode item : items) {
                String title = item.get("title").asText().replaceAll("<[^>]*>", "");
                String description = item.get("description").asText().replaceAll("<[^>]*>", "");

                if (!matchesTeam(title, description, team)) {
                    continue;
                }
                targets.add((ObjectNode) item);
            }

            // 2) 기존 뉴스 조회(있으면 재사용) + 썸네일 스크래핑 대상 선별
            Map<String, NewsEntity> existingByLink = new HashMap<>();
            Map<String, String> thumbByLink = new ConcurrentHashMap<>();
            Set<String> toScrape = new LinkedHashSet<>();

            for (ObjectNode item : targets) {
                String link = item.get("originallink").asText();

                NewsEntity existing = newsRepository.existsByNewsUrl(link)
                        ? newsRepository.findTopByNewsUrl(link)
                        : null;

                if (existing != null) {
                    existingByLink.put(link, existing);
                    String savedThumb = existing.getThumbnailUrl();
                    if (savedThumb != null && !savedThumb.isBlank()) {
                        thumbByLink.put(link, savedThumb);   // DB 썸네일 재사용 → 스크래핑 skip
                    } else {
                        toScrape.add(link);
                    }
                } else {
                    toScrape.add(link);
                }
            }

            // 3) 신규 썸네일만 병렬 스크래핑 (기존엔 10건 동기 호출 → 페이지당 최대 30초)
            toScrape.parallelStream().forEach(link -> {
                String thumb = extractThumbnailUrl(link);
                if (thumb != null && !thumb.isBlank()) {
                    thumbByLink.put(link, thumb);
                }
            });

            // 4) 신규 뉴스만 1회 저장 + 응답 주석
            for (ObjectNode item : targets) {
                String title = item.get("title").asText().replaceAll("<[^>]*>", "");
                String description = item.get("description").asText().replaceAll("<[^>]*>", "");
                String link = item.get("originallink").asText();
                String pubDate = item.get("pubDate").asText();
                String thumbnailUrl = thumbByLink.get(link);

                Long sportId = 1L;
                if (keyword.contains("축구")) {
                    sportId = 1L;
                } else if (keyword.contains("야구")) {
                    sportId = 2L;
                } else if (keyword.contains("LOL") || keyword.contains("롤")) {
                    sportId = 3L;
                }

                String[] parts = link.split("/");
                String source = parts.length > 2 ? parts[2] : "NAVER";

                LocalDateTime publishedAt = ZonedDateTime.parse(
                        pubDate,
                        DateTimeFormatter.RFC_1123_DATE_TIME
                ).toLocalDateTime();

                String detectedTeam = "기타";
                if (title.contains("손흥민") || title.contains("토트넘")) {
                    detectedTeam = "토트넘";
                } else if (title.contains("이강인") || title.contains("PSG")) {
                    detectedTeam = "PSG";
                } else if (title.contains("페이커") || title.contains("T1")) {
                    detectedTeam = "T1";
                }

                NewsEntity savedNews = existingByLink.get(link);

                if (savedNews == null) {
                    // 신규 뉴스만 1회 저장 (기존의 중복 insert 제거)
                    NewsEntity news = new NewsEntity();
                    news.setSportId(sportId);
                    news.setCategory(keyword);
                    news.setTitle(title);
                    news.setSummary(description);
                    news.setNewsUrl(link);
                    news.setSource(source);
                    news.setPublishedAt(publishedAt);
                    news.setCreatedAt(LocalDateTime.now());
                    news.setTeam(detectedTeam);
                    news.setThumbnailUrl(thumbnailUrl);
                    savedNews = newsRepository.save(news);
                } else if ((savedNews.getThumbnailUrl() == null || savedNews.getThumbnailUrl().isBlank())
                        && thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                    // 기존 뉴스에 썸네일이 없을 때만 갱신
                    savedNews.setThumbnailUrl(thumbnailUrl);
                    savedNews = newsRepository.save(savedNews);
                }

                Long scrapCount = newsScrapRepository.countByNewsId(savedNews.getNewsId());

                ((ObjectNode) item).put("newsId", savedNews.getNewsId());
                ((ObjectNode) item).put("thumbnailUrl", thumbnailUrl);
                ((ObjectNode) item).put("scrapCount", scrapCount);
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

    private boolean matchesTeam(String title, String description, String team) {
        if (team == null || team.isBlank()) {
            return true;
        }

        String text = (title + " " + description)
                .replaceAll("<[^>]*>", "")
                .replace(" ", "")
                .toLowerCase();

        String normalizedTeam = team
                .replace(" ", "")
                .toLowerCase();

        if (text.contains(normalizedTeam)) {
            return true;
        }

        return switch (team) {
            // 축구
            case "강원FC" -> text.contains("강원fc");
            case "광주FC" -> text.contains("광주fc");
            case "김천상무프로축구단" -> text.contains("김천상무");
            case "대전 하나 시티즌" -> text.contains("대전하나시티즌");
            case "부천 FC" -> text.contains("부천fc");
            case "서울 이랜드 FC" -> text.contains("서울이랜드");
            case "FC안양" -> text.contains("fc안양");
            case "울산 HD FC" -> text.contains("울산hd");
            case "인천 유나이티드 FC" -> text.contains("인천유나이티드");
            case "전북 현대 모터스" -> text.contains("전북현대");
            case "제주 유나이티드" -> text.contains("제주유나이티드");
            case "포항 스틸러스" -> text.contains("포항스틸러스");

            // 야구
            case "LG 트윈스" -> text.contains("lg트윈스") || text.contains("엘지트윈스");
            case "KIA 타이거즈" -> text.contains("kia타이거즈");
            case "한화 이글스" -> text.contains("한화이글스");
            case "두산 베어스" -> text.contains("두산베어스");
            case "KT 워즈" -> text.contains("kt위즈") || text.contains("ktwiz");
            case "NC 다이노스" -> text.contains("nc다이노스");
            case "SSG 랜더스" -> text.contains("ssg랜더스");
            case "롯데 자이언츠" -> text.contains("롯데자이언츠");
            case "삼성 라이온즈" -> text.contains("삼성라이온즈");
            case "키움 히어로즈" -> text.contains("키움히어로즈");

            // LOL
            case "T1" -> text.contains("t1") || text.contains("티원") || text.contains("페이커");
            case "GEN" -> text.contains("gen") || text.contains("젠지") || text.contains("geng");
            case "HLE" -> text.contains("hle") || text.contains("한화생명") || text.contains("한화생명e스포츠");
            case "BFX" -> text.contains("bfx") || text.contains("bnk피어엑스") || text.contains("피어엑스");
            case "BRO" -> text.contains("bro") || text.contains("브리온");
            case "DK" -> text.contains("dk") || text.contains("디플러스기아") || text.contains("디플러스");
            case "KRX" -> text.contains("krx") || text.contains("drx") || text.contains("디알엑스");
            case "KT 롤스터" -> text.contains("kt롤스터") || text.contains("롤스터");
            case "농심 레드포스" -> text.contains("농심레드포스") || text.contains("농심") || text.contains("레드포스");
            case "DNS" -> text.contains("dns") || text.contains("dnfreecs") || text.contains("dn프릭스") || text.contains("광동프릭스");

            default -> false;
        };
    }
}
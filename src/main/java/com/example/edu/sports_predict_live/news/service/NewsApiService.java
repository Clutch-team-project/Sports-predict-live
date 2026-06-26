package com.example.edu.sports_predict_live.news.service;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import com.example.edu.sports_predict_live.news.repository.NewsRepository;
import com.example.edu.sports_predict_live.news.repository.NewsScrapRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NewsApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final NewsRepository newsRepository;
    private final NewsScrapRepository newsScrapRepository;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    private static final String NAVER_NEWS_URL = "https://openapi.naver.com/v1/search/news.json";
    private static final List<String> DEFAULT_KEYWORDS = List.of("축구", "야구", "LOL");
    private static final Map<String, Long> SPORT_ID_MAP = Map.of(
            "축구", 1L, "야구", 2L, "LOL", 3L, "롤", 3L
    );

    public JsonNode searchSportsNews(String keyword, int page, String team) {
        int start = (page - 1) * 10 + 1;
        String url = NAVER_NEWS_URL + "?query=" + keyword + "&display=10&start=" + start + "&sort=date";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class
        );

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode items = jsonNode.get("items");

            // 팀 필터 통과 항목 수집
            List<ObjectNode> targets = new ArrayList<>();
            for (JsonNode item : items) {
                String title = item.get("title").asText().replaceAll("<[^>]*>", "");
                String description = item.get("description").asText().replaceAll("<[^>]*>", "");
                if (matchesTeam(title, description, team)) {
                    targets.add((ObjectNode) item);
                }
            }

            // 기존 뉴스 조회(있으면 재사용) + 썸네일 스크래핑 대상 선별
            Map<String, NewsEntity> existingByLink = new HashMap<>();
            Map<String, String> thumbByLink = new ConcurrentHashMap<>();
            Set<String> toScrape = new LinkedHashSet<>();

            for (ObjectNode item : targets) {
                String link = item.get("originallink").asText();
                newsRepository.findByNewsUrl(link).ifPresentOrElse(
                        existing -> {
                            existingByLink.put(link, existing);
                            String saved = existing.getThumbnailUrl();
                            if (saved != null && !saved.isBlank()) {
                                thumbByLink.put(link, saved);
                            } else {
                                toScrape.add(link);
                            }
                        },
                        () -> toScrape.add(link)
                );
            }

            // 신규 썸네일만 병렬 스크래핑
            toScrape.parallelStream().forEach(link -> {
                String thumb = extractThumbnailUrl(link);
                if (thumb != null && !thumb.isBlank()) {
                    thumbByLink.put(link, thumb);
                }
            });

            Long sportId = resolveSportId(keyword);

            for (ObjectNode item : targets) {
                String title = item.get("title").asText().replaceAll("<[^>]*>", "");
                String link = item.get("originallink").asText();
                String pubDate = item.get("pubDate").asText();
                String thumbnailUrl = thumbByLink.get(link);
                String source = link.split("/").length > 2 ? link.split("/")[2] : "NAVER";
                LocalDateTime publishedAt = ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toLocalDateTime();

                NewsEntity savedNews = existingByLink.get(link);
                if (savedNews == null) {
                    String description = item.get("description").asText().replaceAll("<[^>]*>", "");
                    NewsEntity news = new NewsEntity();
                    news.setSportId(sportId);
                    news.setCategory(keyword);
                    news.setTitle(title);
                    news.setSummary(description);
                    news.setNewsUrl(link);
                    news.setSource(source);
                    news.setPublishedAt(publishedAt);
                    news.setCreatedAt(LocalDateTime.now());
                    news.setTeam(detectTeam(title));
                    news.setThumbnailUrl(thumbnailUrl);
                    savedNews = newsRepository.save(news);
                } else if ((savedNews.getThumbnailUrl() == null || savedNews.getThumbnailUrl().isBlank())
                        && thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                    savedNews.setThumbnailUrl(thumbnailUrl);
                    savedNews = newsRepository.save(savedNews);
                }

                Long scrapCount = newsScrapRepository.countByNewsId(savedNews.getNewsId());
                item.put("newsId", savedNews.getNewsId());
                item.put("thumbnailUrl", thumbnailUrl);
                item.put("scrapCount", scrapCount);
            }

            return jsonNode;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("뉴스 JSON 파싱 실패", e);
        }
    }

    // 3개 종목 키워드를 병렬로 검색해서 합침
    public JsonNode searchDefaultSportsNews(int page) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode mergedItems = objectMapper.createArrayNode();

        List<CompletableFuture<JsonNode>> futures = DEFAULT_KEYWORDS.stream()
                .map(keyword -> CompletableFuture.supplyAsync(() -> searchSportsNews(keyword, page, null)))
                .toList();

        futures.stream()
                .map(CompletableFuture::join)
                .filter(response -> response != null && response.has("items"))
                .forEach(response -> response.get("items").forEach(mergedItems::add));

        result.set("items", mergedItems);
        return result;
    }

    private Long resolveSportId(String keyword) {
        return SPORT_ID_MAP.entrySet().stream()
                .filter(e -> keyword.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(1L);
    }

    private String detectTeam(String title) {
        if (title.contains("손흥민") || title.contains("토트넘")) return "토트넘";
        if (title.contains("이강인") || title.contains("PSG")) return "PSG";
        if (title.contains("페이커") || title.contains("T1")) return "T1";
        return "기타";
    }

    private String extractThumbnailUrl(String articleUrl) {
        try {
            Document document = Jsoup.connect(articleUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(3000)
                    .get();
            String imageUrl = document.select("meta[property=og:image]").attr("content");
            return imageUrl.isBlank() ? null : imageUrl;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchesTeam(String title, String description, String team) {
        if (team == null || team.isBlank()) return true;

        String text = (title + " " + description).replaceAll("<[^>]*>", "").replace(" ", "").toLowerCase();
        String normalizedTeam = team.replace(" ", "").toLowerCase();

        if (text.contains(normalizedTeam)) return true;

        return switch (team) {
            // 축구
            case "강원FC"           -> text.contains("강원fc");
            case "광주FC"           -> text.contains("광주fc");
            case "김천상무프로축구단" -> text.contains("김천상무");
            case "대전 하나 시티즌"  -> text.contains("대전하나시티즌");
            case "부천 FC"          -> text.contains("부천fc");
            case "서울 이랜드 FC"   -> text.contains("서울이랜드");
            case "FC안양"           -> text.contains("fc안양");
            case "울산 HD FC"       -> text.contains("울산hd");
            case "인천 유나이티드 FC" -> text.contains("인천유나이티드");
            case "전북 현대 모터스"  -> text.contains("전북현대");
            case "제주 유나이티드"   -> text.contains("제주유나이티드");
            case "포항 스틸러스"     -> text.contains("포항스틸러스");
            // 야구
            case "LG 트윈스"  -> text.contains("lg트윈스") || text.contains("엘지트윈스");
            case "KIA 타이거즈" -> text.contains("kia타이거즈");
            case "한화 이글스" -> text.contains("한화이글스");
            case "두산 베어스" -> text.contains("두산베어스");
            case "KT 워즈"    -> text.contains("kt위즈") || text.contains("ktwiz");
            case "NC 다이노스" -> text.contains("nc다이노스");
            case "SSG 랜더스" -> text.contains("ssg랜더스");
            case "롯데 자이언츠" -> text.contains("롯데자이언츠");
            case "삼성 라이온즈" -> text.contains("삼성라이온즈");
            case "키움 히어로즈" -> text.contains("키움히어로즈");
            // LOL
            case "T1"       -> text.contains("t1") || text.contains("티원") || text.contains("페이커");
            case "GEN"      -> text.contains("gen") || text.contains("젠지") || text.contains("geng");
            case "HLE"      -> text.contains("hle") || text.contains("한화생명") || text.contains("한화생명e스포츠");
            case "BFX"      -> text.contains("bfx") || text.contains("bnk피어엑스") || text.contains("피어엑스");
            case "BRO"      -> text.contains("bro") || text.contains("브리온");
            case "DK"       -> text.contains("dk") || text.contains("디플러스기아") || text.contains("디플러스");
            case "KRX"      -> text.contains("krx") || text.contains("drx") || text.contains("디알엑스");
            case "KT 롤스터" -> text.contains("kt롤스터") || text.contains("롤스터");
            case "농심 레드포스" -> text.contains("농심레드포스") || text.contains("농심") || text.contains("레드포스");
            case "DNS"      -> text.contains("dns") || text.contains("dnfreecs") || text.contains("dn프릭스") || text.contains("광동프릭스");
            default -> false;
        };
    }
}

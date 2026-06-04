package com.example.edu.sports_predict_live.news.controller;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import com.example.edu.sports_predict_live.news.service.NewsApiService;
import com.example.edu.sports_predict_live.news.service.NewsService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NewsApiService newsApiService;

    /**
     * 뉴스 저장
     */
    @PostMapping("/save")
    public NewsEntity saveNews(

            @RequestParam Long sportId,
            @RequestParam String category,
            @RequestParam String title,
            @RequestParam String summary,
            @RequestParam String source,
            @RequestParam String newsUrl
    ) {

        return newsService.saveNews(
                sportId,
                category,
                title,
                summary,
                source,
                newsUrl,
                LocalDateTime.now()
        );
    }

    /**
     * 최신 뉴스 조회
     */
    @GetMapping("/latest")
    public List<NewsEntity> getLatestNews() {

        return newsService.getLatestNews();
    }

    /**
     * 종목별 뉴스 조회
     */
    @GetMapping("/sport")
    public List<NewsEntity> getNewsBySport(
            @RequestParam Long sportId
    ) {

        return newsService.getNewsBySport(sportId);
    }

    /**
     * 카테고리별 뉴스 조회
     */
    @GetMapping("/category")
    public List<NewsEntity> getNewsByCategory(
            @RequestParam String category
    ) {

        return newsService.getNewsByCategory(category);
    }

    @GetMapping("/api-test")
    public JsonNode apiTest(
            @RequestParam String keyword
    ) {

        return newsApiService.searchSportsNews(keyword);
    }

    /**
     * 종목 + 카테고리 뉴스 조회
     */
    @GetMapping("/filter")
    public List<NewsEntity> getFilteredNews(

            @RequestParam Long sportId,
            @RequestParam String category
    ) {

        return newsService.getFilteredNews(
                sportId,
                category
        );
    }
}

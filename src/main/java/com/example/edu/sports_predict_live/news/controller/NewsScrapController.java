package com.example.edu.sports_predict_live.news.controller;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import com.example.edu.sports_predict_live.news.entity.NewsScrapEntity;
import com.example.edu.sports_predict_live.news.service.NewsScrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news-scrap")
@RequiredArgsConstructor
public class NewsScrapController {

    private final NewsScrapService newsScrapService;

    /**
     * 뉴스 스크랩 저장
     */
    @GetMapping("/save") //테스트용 후에 post로 변경
    public NewsScrapEntity scrapNews(

            @RequestParam Long userId,
            @RequestParam Long newsId
    ) {

        return newsScrapService.scrapNews(
                userId,
                newsId
        );
    }

    /**
     * 회원별 스크랩 뉴스 조회
     */
    @GetMapping("/user")
    public List<NewsScrapEntity> getUserScrapNews(

            @RequestParam Long userId
    ) {

        return newsScrapService.getUserScrapNews(
                userId
        );
    }

    @GetMapping("/delete")
    public String deleteScrap(

            @RequestParam Long userId,
            @RequestParam Long newsId
    ) {

        newsScrapService.deleteScrap(
                userId,
                newsId
        );

        return "스크랩 삭제 완료";
    }

    @GetMapping("/my-news")
    public List<NewsEntity> getMyScrapNews(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "latest") String order,
            @RequestParam(required = false, defaultValue = "1") int page
    ) {
        return newsScrapService.getMyScrapNews(userId, order, page);
    }
}
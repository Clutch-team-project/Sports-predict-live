package com.example.edu.sports_predict_live.news.service;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import com.example.edu.sports_predict_live.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    /**
     * 뉴스 저장
     */
    public NewsEntity saveNews(
            Long sportId,
            String category,
            String title,
            String summary,
            String source,
            String newsUrl,
            LocalDateTime publishedAt
    ) {

        // 중복 뉴스 방지
        if (newsRepository.findByNewsUrl(newsUrl).isPresent()) {
            throw new RuntimeException("이미 저장된 뉴스입니다.");
        }

        NewsEntity news = new NewsEntity();

        news.setSportId(sportId);
        news.setCategory(category);
        news.setTitle(title);
        news.setSummary(summary);
        news.setSource(source);
        news.setNewsUrl(newsUrl);
        news.setPublishedAt(publishedAt);

        news.setCreatedAt(LocalDateTime.now());

        return newsRepository.save(news);
    }

    /**
     * 최신 뉴스 조회
     */
    public List<NewsEntity> getLatestNews() {

        return newsRepository.findTop10ByOrderByPublishedAtDesc();
    }

    /**
     * 종목별 뉴스 조회
     */
    public List<NewsEntity> getNewsBySport(Long sportId) {

        return newsRepository.findBySportIdOrderByPublishedAtDesc(sportId);
    }

    /**
     * 카테고리별 뉴스 조회
     */
    public List<NewsEntity> getNewsByCategory(String category) {

        return newsRepository.findByCategoryOrderByPublishedAtDesc(category);
    }

    /**
     * 종목 + 카테고리 뉴스 조회
     */
    public List<NewsEntity> getFilteredNews(
            Long sportId,
            String category
    ) {

        return newsRepository
                .findBySportIdAndCategoryOrderByPublishedAtDesc(
                        sportId,
                        category
                );
    }

    public List<NewsEntity> getNewsBySportAndTeam(
            Long sportId,
            String team
    ) {

        return newsRepository
                .findBySportIdAndTeamOrderByPublishedAtDesc(
                        sportId,
                        team
                );
    }
}
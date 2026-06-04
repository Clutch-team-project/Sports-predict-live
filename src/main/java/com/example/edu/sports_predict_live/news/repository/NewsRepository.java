package com.example.edu.sports_predict_live.news.repository;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRepository
        extends JpaRepository<NewsEntity, Long> {

    // 최신 뉴스 조회
    List<NewsEntity> findTop10ByOrderByPublishedAtDesc();

    // 종목별 뉴스 조회
    List<NewsEntity> findBySportIdOrderByPublishedAtDesc(Long sportId);

    // 카테고리별 뉴스 조회
    List<NewsEntity> findByCategoryOrderByPublishedAtDesc(String category);

    // URL 중복 체크
    boolean existsByNewsUrl(String newsUrl);

    // 종목 + 팀 조회
    List<NewsEntity> findBySportIdAndCategoryOrderByPublishedAtDesc(
            Long sportId,
            String category
    );
}
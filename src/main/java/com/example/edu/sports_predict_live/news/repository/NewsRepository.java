package com.example.edu.sports_predict_live.news.repository;

import com.example.edu.sports_predict_live.news.entity.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRepository
        extends JpaRepository<NewsEntity, Long> {

    // 최신 뉴스 조회
    List<NewsEntity> findTop10ByOrderByPublishedAtDesc();

    // 종목별 뉴스 조회 (최대 20건)
    List<NewsEntity> findTop20BySportIdOrderByPublishedAtDesc(Long sportId);

    // 카테고리별 뉴스 조회 (최대 20건)
    List<NewsEntity> findTop20ByCategoryOrderByPublishedAtDesc(String category);

    // URL 중복 체크
    boolean existsByNewsUrl(String newsUrl);

    // 종목 + 카테고리 조회 (최대 20건)
    List<NewsEntity> findTop20BySportIdAndCategoryOrderByPublishedAtDesc(
            Long sportId,
            String category
    );

    // 종목 + 팀 조회 (최대 20건)
    List<NewsEntity> findTop20BySportIdAndTeamOrderByPublishedAtDesc(
            Long sportId,
            String team
    );

    NewsEntity findTopByNewsUrl(String newsUrl);

}
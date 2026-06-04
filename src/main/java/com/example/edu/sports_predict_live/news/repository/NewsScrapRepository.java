package com.example.edu.sports_predict_live.news.repository;

import com.example.edu.sports_predict_live.news.entity.NewsScrapEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsScrapRepository
        extends JpaRepository<NewsScrapEntity, Long> {

    // 회원별 스크랩 조회
    List<NewsScrapEntity> findByUserId(Long userId);

    // 중복 스크랩 체크
    boolean existsByUserIdAndNewsId(
            Long userId,
            Long newsId
    );

    void deleteByUserIdAndNewsId(
            Long userId,
            Long newsId
    );
}
package com.example.edu.sports_predict_live.news.service;

import com.example.edu.sports_predict_live.news.entity.NewsScrapEntity;
import com.example.edu.sports_predict_live.news.repository.NewsScrapRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class NewsScrapService {

    private final NewsScrapRepository newsScrapRepository;

    /**
     * 뉴스 스크랩 저장
     */
    public NewsScrapEntity scrapNews(
            Long userId,
            Long newsId
    ) {

        // 중복 스크랩 체크
        boolean exists =
                newsScrapRepository.existsByUserIdAndNewsId(
                        userId,
                        newsId
                );

        if (exists) {

            throw new RuntimeException("이미 스크랩한 뉴스입니다.");
        }

        NewsScrapEntity newsScrapEntity =
                new NewsScrapEntity();

        newsScrapEntity.setUserId(userId);
        newsScrapEntity.setNewsId(newsId);
        newsScrapEntity.setCreatedAt(LocalDateTime.now());

        return newsScrapRepository.save(newsScrapEntity);
    }

    /**
     * 회원별 스크랩 뉴스 조회
     */
    public List<NewsScrapEntity> getUserScrapNews(
            Long userId
    ) {

        return newsScrapRepository.findByUserId(userId);
    }
    //스크랩 뉴스 삭제
    @Transactional
    public void deleteScrap(
            Long userId,
            Long newsId
    ) {

        newsScrapRepository.deleteByUserIdAndNewsId(
                userId,
                newsId
        );
    }
}
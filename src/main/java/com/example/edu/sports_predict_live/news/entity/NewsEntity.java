package com.example.edu.sports_predict_live.news.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news", indexes = {
        @Index(name = "idx_news_sport_published", columnList = "sport_id, published_at"),
        @Index(name = "idx_news_category_published", columnList = "category, published_at"),
        @Index(name = "idx_news_published", columnList = "published_at"),
        @Index(name = "idx_news_url", columnList = "news_url", unique = true)
})
@Getter
@Setter
public class NewsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "sport_id")
    private Long sportId;

    @Column(name = "category")
    private String category;

    @Column(name = "title")
    private String title;

    @Column(name = "summary")
    private String summary;

    @Column(name = "source")
    private String source;

    @Column(name = "news_url")
    private String newsUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String team;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;
}
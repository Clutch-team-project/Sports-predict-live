package com.example.edu.sports_predict_live.news.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_scrap")
@Getter
@Setter
public class NewsScrapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_scrap_id")
    private Long newsScrapId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
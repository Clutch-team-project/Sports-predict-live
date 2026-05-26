package com.example.edu.sports_predict_live.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 20)
    private String loginId;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(unique = true, length = 20)
    private String nickname;

    @Column(length = 20)
    private String phone;

    private LocalDate birthDate;

    @Column(length = 500)
    private String profileImage;

    @Column(nullable = false)
    private boolean marketingAgreed = false;

    @Column(nullable = false)
    private boolean matchStartAlert = true;

    @Column(nullable = false)
    private boolean predictionResultAlert = true;

    @Column(nullable = false)
    private int alertBeforeMinutes = 30;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public User(String loginId, String email, String password,
                String name, String nickname, String phone, LocalDate birthDate) {
        this.loginId = loginId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.birthDate = birthDate;
    }
}
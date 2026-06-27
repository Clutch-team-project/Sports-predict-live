package com.example.edu.sports_predict_live.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 회원 — 일반(LOCAL) 및 소셜(GOOGLE/KAKAO) 계정 통합, 탈퇴는 deletedAt 소프트 삭제
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 50)
    private String loginId;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider = Provider.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

    @Column(length = 100)
    private String socialId;

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
    private boolean matchStartAlert = false;

    @Column(nullable = false)
    private boolean predictionResultAlert = false;

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
                String name, String nickname, String phone, LocalDate birthDate,
                Provider provider, String socialId, String profileImage,
                boolean marketingAgreed, boolean matchStartAlert, boolean predictionResultAlert) {
        this.loginId = loginId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.birthDate = birthDate;
        this.provider = provider != null ? provider : Provider.LOCAL;
        this.socialId = socialId;
        this.profileImage = profileImage;
        this.marketingAgreed = marketingAgreed;
        this.matchStartAlert = matchStartAlert;
        this.predictionResultAlert = predictionResultAlert;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateAlertSettings(Boolean marketingAgreed, Boolean matchStartAlert,
                                    Boolean predictionResultAlert, Integer alertBeforeMinutes) {
        if (marketingAgreed != null) this.marketingAgreed = marketingAgreed;
        if (matchStartAlert != null) this.matchStartAlert = matchStartAlert;
        if (predictionResultAlert != null) this.predictionResultAlert = predictionResultAlert;
        if (alertBeforeMinutes != null) this.alertBeforeMinutes = alertBeforeMinutes;
    }

    public void updateProfile(String name, String nickname, String phone,
                              LocalDate birthDate, String profileImage, boolean removeProfileImage) {
        if (name != null && !name.isBlank()) this.name = name;
        this.nickname  = (nickname != null && !nickname.isBlank()) ? nickname : null;
        this.phone     = (phone != null && !phone.isBlank()) ? phone : null;
        this.birthDate = birthDate;
        if (removeProfileImage) this.profileImage = null;
        else if (profileImage != null) this.profileImage = profileImage;
    }

    public void updateSocialId(String socialId) {
        this.socialId = socialId;
    }

    public void delete() {
        this.email    = "deleted_" + this.userId + "@deleted.com";
        this.loginId  = "deleted_" + this.userId;
        this.nickname = null;
        this.socialId = null;   // 소셜 재로그인 시 탈퇴 계정과 매칭되지 않도록 해제
        this.deletedAt = LocalDateTime.now();
    }
}
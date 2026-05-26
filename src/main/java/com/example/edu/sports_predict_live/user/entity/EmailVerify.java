package com.example.edu.sports_predict_live.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verify")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerify {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emailVerifyId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 10)
    private String verifyCode;

    @Column(nullable = false, length = 20)
    private String purpose;

    @Column(nullable = false)
    private int attemptCount = 0;

    @Column(nullable = false)
    private boolean isVerified = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public EmailVerify(String email, String verifyCode,
                       String purpose, LocalDateTime expiresAt) {
        this.email = email;
        this.verifyCode = verifyCode;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public void markVerified() {
        this.isVerified = true;
    }

    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isMaxAttempt() {
        return this.attemptCount >= 5;
    }
}
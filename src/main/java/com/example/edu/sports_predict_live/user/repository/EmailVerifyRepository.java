package com.example.edu.sports_predict_live.user.repository;

import com.example.edu.sports_predict_live.user.entity.EmailVerify;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerifyRepository extends JpaRepository<EmailVerify, Long> {
    Optional<EmailVerify> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email, String purpose);
}
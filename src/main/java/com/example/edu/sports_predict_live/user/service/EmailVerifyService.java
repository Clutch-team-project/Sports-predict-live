package com.example.edu.sports_predict_live.user.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.user.entity.EmailVerify;
import com.example.edu.sports_predict_live.user.repository.EmailVerifyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerifyService {

    private final EmailVerifyRepository emailVerifyRepository;
    private final JavaMailSender mailSender;

    public void sendCode(String email, String purpose) {
        String code = generateCode();

        EmailVerify verify = EmailVerify.builder()
                .email(email)
                .verifyCode(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        emailVerifyRepository.save(verify);
        sendMail(email, code);
    }

    public void verifyCode(String email, String code, String purpose) {
        EmailVerify verify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFY_NOT_FOUND));

        if (verify.isMaxAttempt())
            throw new CustomException(ErrorCode.VERIFY_MAX_ATTEMPT);
        if (verify.isExpired())
            throw new CustomException(ErrorCode.VERIFY_EXPIRED);
        if (!verify.getVerifyCode().equals(code)) {
            verify.increaseAttemptCount();
            throw new CustomException(ErrorCode.VERIFY_CODE_MISMATCH);
        }

        verify.markVerified();
    }

    private String generateCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private void sendMail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[AI.MATCH] 이메일 인증코드");
        message.setText("인증코드: " + code + "\n\n10분 내에 입력해주세요.");
        mailSender.send(message);
    }
}
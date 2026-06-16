package com.example.edu.sports_predict_live.user.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.user.dto.request.AdminCreateUserRequestDTO;
import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 전용 — 이메일 인증 없이 계정 직접 생성
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO createUser(AdminCreateUserRequestDTO dto) {
        if (userRepository.existsByLoginId(dto.getLoginId()))
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        if (dto.getNickname() != null && !dto.getNickname().isBlank()
                && userRepository.existsByNickname(dto.getNickname()))
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);

        User user = User.builder()
                .loginId(dto.getLoginId())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname() != null && !dto.getNickname().isBlank()
                        ? dto.getNickname() : null)
                .build();

        return UserResponseDTO.from(userRepository.save(user));
    }
}

package com.example.edu.sports_predict_live.user.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.user.dto.request.LoginRequestDTO;
import com.example.edu.sports_predict_live.user.dto.request.SignupRequestDTO;
import com.example.edu.sports_predict_live.user.dto.response.ReissueResponseDTO;
import com.example.edu.sports_predict_live.user.dto.response.TokenResponseDTO;
import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.entity.EmailVerify;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.EmailVerifyRepository;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerifyRepository emailVerifyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public UserResponseDTO signup(SignupRequestDTO dto) {
        // 이메일 인증 완료 여부 확인
        EmailVerify verify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(dto.getEmail(), "signup")
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_VERIFIED));

        if (!verify.isVerified())
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);

        // 중복 검사
        if (userRepository.existsByLoginId(dto.getLoginId()))
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        if (userRepository.existsByEmail(dto.getEmail()))
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        if (dto.getNickname() != null && userRepository.existsByNickname(dto.getNickname()))
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);

        User user = User.builder()
                .loginId(dto.getLoginId())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate())
                .build();

        return UserResponseDTO.from(userRepository.save(user));
    }

    public TokenResponseDTO login(LoginRequestDTO dto) {
        // 회원 조회
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(dto.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 확인
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
            throw new CustomException(ErrorCode.INVALID_PASSWORD);

        // 토큰 발급
        String accessToken  = jwtProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        return new TokenResponseDTO(
                accessToken,
                refreshToken,
                user.getUserId(),
                user.getNickname(),
                user.getLoginId()
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDTO.from(user);
    }

    public ReissueResponseDTO reissue(String refreshToken) {
        // 토큰 유효성 검사
        if (!jwtProvider.validateToken(refreshToken))
            throw new CustomException(ErrorCode.INVALID_TOKEN);

        // Refresh Token 타입 확인
        if (!jwtProvider.isRefreshToken(refreshToken))
            throw new CustomException(ErrorCode.INVALID_TOKEN);

        // 회원 조회
        Long userId = jwtProvider.getUserId(refreshToken);
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새 토큰 발급
        String newAccessToken  = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        return new ReissueResponseDTO(newAccessToken, newRefreshToken);
    }
}
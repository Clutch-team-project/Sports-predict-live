package com.example.edu.sports_predict_live.user.service;

import com.example.edu.sports_predict_live.global.exception.CustomException;
import com.example.edu.sports_predict_live.global.exception.ErrorCode;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.user.dto.request.*;
import com.example.edu.sports_predict_live.user.dto.response.ReissueResponseDTO;
import com.example.edu.sports_predict_live.user.dto.response.TokenResponseDTO;
import com.example.edu.sports_predict_live.user.dto.response.UserResponseDTO;
import com.example.edu.sports_predict_live.user.entity.EmailVerify;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.EmailVerifyRepository;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

// 회원 가입/로그인/토큰 재발급/내 정보 관리 (조회·수정·비밀번호 변경·탈퇴)
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerifyRepository emailVerifyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${com.example.upload.path}")
    private String uploadPath;

    public UserResponseDTO signup(SignupRequestDTO dto) {
        EmailVerify verify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(dto.getEmail(), "signup")
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_VERIFIED));

        if (!verify.isVerified())
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);

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
                .marketingAgreed(dto.isMarketingAgreed())
                .matchStartAlert(dto.isMatchStartAlert())
                .predictionResultAlert(dto.isPredictionResultAlert())
                .build();

        return UserResponseDTO.from(userRepository.save(user));
    }

    public TokenResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(dto.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword()))
            throw new CustomException(ErrorCode.INVALID_PASSWORD);

        String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getRole().name());
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
        if (!jwtProvider.validateToken(refreshToken))
            throw new CustomException(ErrorCode.INVALID_TOKEN);

        if (!jwtProvider.isRefreshToken(refreshToken))
            throw new CustomException(ErrorCode.INVALID_TOKEN);

        Long userId = jwtProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        return new ReissueResponseDTO(newAccessToken, newRefreshToken);
    }

    @Transactional(readOnly = true)
    public Map<String, String> findLoginId(String email, String code) {
        EmailVerify verify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, "find_id")
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFY_NOT_FOUND));

        if (!verify.isVerified())
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

        return Map.of("loginId", user.getLoginId());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        EmailVerify verify = emailVerifyRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(dto.getEmail(), "reset_pw")
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFY_NOT_FOUND));

        if (!verify.isVerified())
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);

        User user = userRepository.findByEmailAndDeletedAtIsNull(dto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
    }

    @Transactional
    public UserResponseDTO updateMe(Long userId, UserUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (dto.getNickname() != null && !dto.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(dto.getNickname()))
                throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String savedImagePath = saveProfileImage(dto.getProfileImage(), user.getProfileImage());

        user.updateProfile(dto.getNickname(), dto.getPhone(), dto.getBirthDate(), savedImagePath);
        user.updateAlertSettings(dto.getMarketingAgreed(), dto.getMatchStartAlert(),
                dto.getPredictionResultAlert(), dto.getAlertBeforeMinutes());

        return UserResponseDTO.from(user);
    }

    private String saveProfileImage(MultipartFile file, String existingImagePath) {
        if (file == null || file.isEmpty()) {
            return null; // null이면 User.updateProfile()에서 기존 값 유지
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }

        String profileDir = Paths.get(uploadPath, "profile").toAbsolutePath().toString();
        File dir = new File(profileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 기존 프로필 이미지 삭제 (소셜 로그인의 외부 URL은 로컬 파일이 없으므로 건너뜀)
        if (existingImagePath != null && !existingImagePath.startsWith("http")) {
            File oldFile = new File(Paths.get(uploadPath, "profile", existingImagePath).toString());
            if (oldFile.exists()) {
                oldFile.delete();
            }
        }

        String originalName = file.getOriginalFilename();
        String saveName = UUID.randomUUID() + "_" + originalName;
        try {
            file.transferTo(Paths.get(profileDir, saveName));
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장 중 오류가 발생했습니다.", e);
        }

        return saveName;
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword()))
            throw new CustomException(ErrorCode.INVALID_PASSWORD);

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword()))
            throw new CustomException(ErrorCode.SAME_PASSWORD);

        user.updatePassword(passwordEncoder.encode(dto.getNewPassword()));
    }

    @Transactional
    public void deleteMe(Long userId, DeleteUserRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null)
            throw new CustomException(ErrorCode.ALREADY_DELETED);

        // 소셜 전용 계정이 아니면 비밀번호 확인
        if (user.getPassword() != null) {
            if (dto.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword()))
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.delete();
    }
}
package com.example.edu.sports_predict_live.global.oauth2;

import com.example.edu.sports_predict_live.user.entity.Provider;
import com.example.edu.sports_predict_live.user.entity.User;
import com.example.edu.sports_predict_live.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

// 소셜 로그인 유저 로드 — 기존 계정 연결 또는 신규 가입 처리
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User.getAttributes());
        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        User user = findOrCreate(userInfo, provider);
        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"), "지원하지 않는 소셜 로그인: " + registrationId);
        };
    }

    private User findOrCreate(OAuth2UserInfo info, Provider provider) {
        // 1. socialId + provider로 조회 (정상 재로그인) — 탈퇴 계정 제외
        Optional<User> bySocial = userRepository
                .findBySocialIdAndProviderAndDeletedAtIsNull(info.getSocialId(), provider);
        if (bySocial.isPresent()) return bySocial.get();

        // 2. 이메일로 조회 (socialId 미저장 등 엣지케이스 대응)
        if (info.getEmail() != null) {
            Optional<User> byEmail = userRepository.findByEmailAndDeletedAtIsNull(info.getEmail());
            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                if (existing.getProvider() == provider) {
                    // 같은 소셜 provider인데 socialId만 없는 경우 → 복구 후 로그인
                    existing.updateSocialId(info.getSocialId());
                    return userRepository.save(existing);
                }
                // 일반(LOCAL) 계정 또는 다른 소셜 provider 계정
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("email_already_exists"),
                        "이미 가입된 이메일입니다. 기존 방식으로 로그인해주세요.");
            }
        }

        // 3. 신규 생성
        String loginId = provider.name().toLowerCase() + "_" + info.getSocialId();
        if (loginId.length() > 50) loginId = loginId.substring(0, 50);

        User newUser = User.builder()
                .loginId(loginId)
                .email(info.getEmail() != null ? info.getEmail()
                        : provider.name().toLowerCase() + "_" + info.getSocialId() + "@social.com")
                .name(info.getName() != null ? info.getName() : "사용자")
                .nickname(generateUniqueNickname(info.getName()))
                .profileImage(info.getProfileImage())
                .provider(provider)
                .socialId(info.getSocialId())
                .build();

        return userRepository.save(newUser);
    }

    private String generateUniqueNickname(String baseName) {
        String base = (baseName != null && !baseName.isBlank()) ? baseName : "사용자";
        if (base.length() > 15) base = base.substring(0, 15);
        String nickname = base;
        int suffix = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = base + suffix++;
        }
        return nickname;
    }
}

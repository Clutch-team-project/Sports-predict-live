package com.example.edu.sports_predict_live.global.oauth2;

import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// 소셜 로그인 성공 — JWT 발급 후 /login-success로 URL fragment(#)에 담아 전달
// fragment는 서버 액세스 로그와 Referer 헤더에 포함되지 않아 쿼리 파라미터보다 안전
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        String accessToken  = jwtProvider.createAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getUserId());

        String redirectUrl = "/login-success#accessToken="
                + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
                + "&refreshToken="
                + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

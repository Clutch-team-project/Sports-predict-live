package com.example.edu.sports_predict_live.global.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String message = exception.getMessage() != null ? exception.getMessage() : "소셜 로그인에 실패했습니다.";
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);

        String redirectUrl = UriComponentsBuilder.fromUriString("/login")
                .queryParam("error", encoded)
                .build(true)   // 이미 인코딩된 값이므로 추가 인코딩 방지
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

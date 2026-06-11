package com.example.edu.sports_predict_live.global.config;

import com.example.edu.sports_predict_live.global.jwt.JwtFilter;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.global.oauth2.CustomOAuth2UserService;
import com.example.edu.sports_predict_live.global.oauth2.OAuth2FailureHandler;
import com.example.edu.sports_predict_live.global.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    private static final String[] PUBLIC_API = {
            "/api/auth/**",
            "/api/standings/**",
            "/api/records/**",
            "/api/players/**",
            "/api/schedule/**",
            "/prediction/**",
            "/ai-pred/**",
            "/match/**",
            "/games/**",
            "/api/games/**",
            "/oauth2/**",
            "/login/oauth2/**"
    };

    private static final String[] PUBLIC_PAGES = {
            "/",
            "/login", "/signup", "/signup-success", "/login-success",
            "/notification-agreement",
            "/find-id", "/find-password", "/change-password",
            "/prediction-history", "/user-info",
            "/board", "/board/list", "/board/read", "/templates/**",
            "/baseball/**", "/soccer/**", "/lol/**",
    };

    private static final String[] PUBLIC_STATIC = {
            "/gnb.js", "/script.js", "/favicon.ico",
            "/*.js", "/*.css", "/*.png",
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_API).permitAll()
                        .requestMatchers(PUBLIC_PAGES).permitAll()
                        .requestMatchers(PUBLIC_STATIC).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                .addFilterBefore(new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

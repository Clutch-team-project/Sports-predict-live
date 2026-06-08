package com.example.edu.sports_predict_live.global.config;

import com.example.edu.sports_predict_live.global.jwt.JwtFilter;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        .requestMatchers("/api/auth/**").permitAll()
<<<<<<< HEAD
                        .requestMatchers("/prediction/**", "/ai-pred/**", "/match/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/games/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()
=======
                        .requestMatchers("/baseball/**", "/soccer/**", "/lol/**").permitAll()
                        .requestMatchers("/api/standings/**", "/api/records/**", "/api/players/**").permitAll()
>>>>>>> 9fe9c7b4e2f4433654777ad84581f6a1a2ab7bfb
                        .requestMatchers(
                                "/", "/login", "/signup", "/notification-agreement",
                                "/signup-success", "/login-success", "/find-id",
                                "/find-password", "/change-password", "/prediction-history",
                                "/user-info"
                                , "/board", "/board/list", "/board/read", "/templates/**" // ← 추가
                        ).permitAll()
                        .requestMatchers("/script.js", "/gnb.js", "/favicon.ico", "/*.js", "/*.css", "/*.png").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");   // 모든 출처 허용 (개발용)
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

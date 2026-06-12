package com.example.edu.sports_predict_live.global.config;

import com.example.edu.sports_predict_live.global.jwt.JwtFilter;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.global.oauth2.CustomOAuth2UserService;
import com.example.edu.sports_predict_live.global.oauth2.OAuth2FailureHandler;
import com.example.edu.sports_predict_live.global.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    // 공개 API 경로
    private static final String[] PUBLIC_API = {
            "/api/auth/**",
            "/api/standings/**",
            "/api/records/**",
            "/api/players/**",
            "/api/teams/**",
            "/api/schedule/**",

            "/news/**",
            "/news-scrap/**",

            "/oauth2/**",
            "/login/oauth2/**"
    };

    // 공개 페이지 경로
    private static final String[] PUBLIC_PAGES = {
            "/",
            "/login", "/signup", "/signup-success", "/login-success",
            "/notification-agreement",
            "/find-id", "/find-password", "/change-password",
            "/prediction-history", "/user-info", "/favorite-teams",
            "/baseball/**", "/soccer/**", "/lol/**",
            "/team-detail", "/player-detail",
    };

    // 공개 정적 리소스
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
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/baseball/**", "/soccer/**", "/lol/**").permitAll()
                        .requestMatchers("/api/standings/**", "/api/records/**", "/api/players/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/board/register", "board/modify").permitAll()
                        .requestMatchers(
                                "/", "/login", "/signup", "/notification-agreement",
                                "/signup-success", "/login-success", "/find-id",
                                "/find-password", "/change-password", "/prediction-history",
                                "/user-info", "/favorite-teams"
                                , "/board", "/board/list", "/board/read/**", "/templates/**" // ← 추가
                        ).permitAll()
                        .requestMatchers("/script.js", "/gnb.js", "/favicon.ico", "/*.js", "/*.css", "/*.png").permitAll()
                        .requestMatchers(PUBLIC_API).permitAll()
                        .requestMatchers(PUBLIC_PAGES).permitAll()
                        .requestMatchers(PUBLIC_STATIC).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String uri = request.getRequestURI();

                            // API(Ajax/Fetch) 요청인 경우 -> 순수 401 상태 코드만 반환
                            if (uri.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("Unauthorized");
                            }
                            // 화면(HTML) 이동 요청인 경우 -> 자바스크립트 Confirm 창 응답
                            else {
                                response.setContentType("text/html; charset=UTF-8");
                                PrintWriter out = response.getWriter();
                                out.println("<script>");
                                out.println("if(confirm('로그인 후 이용 가능합니다.\\n로그인 페이지로 이동하시겠습니까?')) {");
                                out.println("   location.href='/login';"); // 확인 누르면 로그인 창
                                out.println("} else {");
                                out.println("   location.href='/board/list';"); // 취소 누르면 리스트 복귀
                                out.println("}");
                                out.println("</script>");
                                out.flush();
                            }
                        })
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
        config.addAllowedOriginPattern("*");  // 개발용 — 배포 시 도메인 지정 필요
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
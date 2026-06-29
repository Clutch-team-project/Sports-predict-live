package com.example.edu.sports_predict_live.global.config;

import com.example.edu.sports_predict_live.global.jwt.JwtFilter;
import com.example.edu.sports_predict_live.global.jwt.JwtProvider;
import com.example.edu.sports_predict_live.global.oauth2.CustomOAuth2UserService;
import com.example.edu.sports_predict_live.global.oauth2.OAuth2FailureHandler;
import com.example.edu.sports_predict_live.global.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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

    // CORS 허용 오리진 — application.properties의 cors.allowed-origins (콤마 구분)
    @Value("${cors.allowed-origins:http://localhost:8080,http://localhost:3000}")
    private String[] allowedOrigins;

    // 공개 API 경로
    private static final String[] PUBLIC_API = {
            "/api/auth/**",              // 인증 (이메일 인증, 로그인, 회원가입 등)
            "/api/standings/**",         // 팀 순위
            "/api/records/**",           // 선수 기록
            "/api/players/**",           // 선수 목록
            "/api/teams/**",             // 팀 정보
            "/api/schedule/**",          // 경기 일정
            "/api/predictions/ranking",  // 포인트 순위 (비로그인도 조회 가능)
            "/api/ai-prediction/**",     // AI 승부 예측 (비로그인도 조회 가능)
            "/news/**",
            "/news-scrap/**",
            "/oauth2/**",                // OAuth2 인증
            "/login/oauth2/**",          // OAuth2 콜백
    };

    // 공개 페이지 경로
    private static final String[] PUBLIC_PAGES = {
            "/",
            "/schedule",
            "/login", "/signup", "/signup-success", "/login-success",
            "/notification-agreement",
            "/find-id", "/find-password", "/change-password",
            "/prediction-history", "/user-info", "/favorite-teams",
            "/baseball/**", "/soccer/**", "/lol/**",
            "/board", "/board/list/**", "/board/read/**",
            "/replies/list/**", "/templates/**",
            "/team-detail", "/player-detail", "/news", "/sitemap",
            "/files/**",
    };

    // 공개 정적 리소스
    private static final String[] PUBLIC_STATIC = {
            "/gnb.js", "/board.js", "/notification.js", "/script.js", "/favicon.ico",
            "/*.js", "/*.css", "/*.png",
            "/images/**",
    };

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers("/images/**", "/favicon.ico", "/gnb.js", "/board.js", "/notification.js", "/script.js", "/*.js", "/*.css", "/*.png", "/*.svg");
    }

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
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/baseball/**", "/soccer/**", "/lol/**").permitAll()
                        // /admin/control 등 페이지는 permitAll — admin-control.html에서 JS로 role 체크하고,
                        // 실제 데이터는 /api/admin/** (아래 hasRole(ADMIN))에서 보호됨
                        .requestMatchers("/admin/control").permitAll()
                        .requestMatchers("/api/standings/**", "/api/records/**", "/api/players/**").permitAll()
                        .requestMatchers("/games/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/games/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/board/register", "/board/modify").permitAll()
                        .requestMatchers(
                                "/", "/schedule",
                                "/login", "/signup", "/notification-agreement",
                                "/signup-success", "/login-success", "/find-id",
                                "/find-password", "/change-password", "/prediction-history",
                                "/user-info", "/favorite-teams",
                                "/board", "/board/list/**", "/board/read/**", "/replies/list/**", "/templates/**" // ← 추가
                        ).permitAll()
                        .requestMatchers("/script.js", "/gnb.js", "/board.js", "/notification.js", "/favicon.ico", "/*.js", "/*.css", "/*.png", "/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/board/register", "/board/modify").permitAll()
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
                                // API 요청 → 401 상태 코드만 반환
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("Unauthorized");
                            } else {
                                // 페이지 요청 → 로그인 유도 confirm 창
                                response.setContentType("text/html; charset=UTF-8");
                                PrintWriter out = response.getWriter();
                                out.println("<script>");
                                out.println("if(confirm('로그인 후 이용 가능합니다.\\n로그인 페이지로 이동하시겠습니까?')) {");
                                out.println("   location.href='/login';");
                                out.println("} else {");
                                out.println("   history.back();");
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
        // 와일드카드 + credentials 조합은 CSRF 우회 위험이 있어 명시적 화이트리스트 사용
        for (String origin : allowedOrigins) {
            config.addAllowedOrigin(origin.trim());
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

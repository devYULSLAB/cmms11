package com.cmms11.config;

import com.cmms11.domain.member.Member;
import com.cmms11.domain.member.MemberRepository;
import com.cmms11.security.CsrfCookieFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.time.LocalDateTime;

/**
 * 이름: SecurityConfig
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일:
 * 프로그램 개요: Spring Security 전역 설정 및 CSRF/로그 필터 구성.
 * 처리순서: 로그인폼->/api/auth/login->SecurityConfig->MemberUserDetailsService->AuthenticationManager->SecurityFilterChain->filterChain
 * 
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private MemberRepository memberRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        // Align header name with frontend (app.js uses X-CSRF-TOKEN)
        csrfTokenRepository.setHeaderName("X-CSRF-TOKEN");


        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
            .csrf(csrf -> csrf

                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(requestHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/static/**",
                    "/assets/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/api/health",
                    "/api/auth/logout"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/auth/login.html")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("member_id")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    // ✨ 로그인 성공 처리
                    String username = authentication.getName();
                    String[] parts = username.split(":", 2);
                    String companyId = parts[0];
                    String memberId = parts.length > 1 ? parts[1] : parts[0];
                    
                    // ✨ 마지막 로그인 시간 및 IP 업데이트
                    updateLastLogin(companyId, memberId, request);
                    
                    // ✨ "로그인 정보 저장" 체크박스 체크 시 쿠키 저장
                    String rememberLogin = request.getParameter("remember_login");
                    if ("true".equals(rememberLogin)) {
                        // 30일 유지 쿠키 생성
                        Cookie companyCookie = new Cookie("cmms_company_id", companyId);
                        companyCookie.setPath("/");
                        companyCookie.setMaxAge(30 * 24 * 60 * 60); // 30일
                        companyCookie.setHttpOnly(false); // JavaScript에서 읽기 가능
                        
                        Cookie userCookie = new Cookie("cmms_username", memberId);
                        userCookie.setPath("/");
                        userCookie.setMaxAge(30 * 24 * 60 * 60);
                        userCookie.setHttpOnly(false);
                        
                        Cookie rememberCookie = new Cookie("cmms_remember", "true");
                        rememberCookie.setPath("/");
                        rememberCookie.setMaxAge(30 * 24 * 60 * 60);
                        rememberCookie.setHttpOnly(false);
                        
                        response.addCookie(companyCookie);
                        response.addCookie(userCookie);
                        response.addCookie(rememberCookie);
                        
                        log.info("Login info saved to cookies for user: {}", username);
                    } else {
                        // 체크 안 했으면 기존 쿠키 삭제
                        deleteCookie(response, "cmms_company_id");
                        deleteCookie(response, "cmms_username");
                        deleteCookie(response, "cmms_remember");
                    }
                    
                    log.info("User logged in successfully: {}", username);
                    response.sendRedirect("/layout/defaultLayout.html?content=/memo/list");
                })
                .failureHandler((request, response, exception) -> {
                    // ⭐ 모든 로그인 실패를 동일한 메시지로 통일 (보안 강화)
                    log.warn("Login failed for user: {} - {}", 
                        request.getParameter("member_id"), 
                        exception.getClass().getSimpleName());
                    response.sendRedirect("/auth/login.html?error=1");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new JsonLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "cmms_company_id", "cmms_username", "cmms_remember")
                .clearAuthentication(true)
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/auth/login.html");
                })
                .accessDeniedHandler(accessDeniedHandler())
            );

        http
            .addFilterBefore(new RequestLoggingFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        return http.build();
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            if (response.isCommitted()) {
                return;
            }

            log.warn(
                "Access denied for {} {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getMessage()
            );

            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, UserDetailsService userDetailsService, PasswordEncoder encoder) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(encoder);
        return builder.build();
    }

    /**
     * 마지막 로그인 시간 및 IP 업데이트
     */
    private void updateLastLogin(String companyId, String memberId, HttpServletRequest request) {
        try {
            Member member = memberRepository.findByIdCompanyIdAndIdMemberId(companyId, memberId)
                .orElse(null);
            
            if (member != null) {
                member.setLastLoginAt(LocalDateTime.now());
                member.setLastLoginIp(getClientIpAddress(request));
                memberRepository.save(member);
                log.debug("Updated last login for {}:{}", companyId, memberId);
            }
        } catch (Exception e) {
            // 로그인 실패하지 않도록 예외 무시
            log.warn("Failed to update last login: {}", e.getMessage());
        }
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 여러 IP가 있을 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 쿠키 삭제 헬퍼 메서드
     */
    private void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}

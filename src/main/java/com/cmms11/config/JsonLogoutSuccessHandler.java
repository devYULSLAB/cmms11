package com.cmms11.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 이름: JsonLogoutSuccessHandler
 * 작성자: codex
 * 작성일: 2025-10-07
 * 프로그램 개요: 로그아웃 성공 시 JSON 응답을 반환하는 핸들러.
 * API 호출(/api/auth/logout)에서는 JSON을 반환하고,
 * 일반 폼 제출에서는 로그인 페이지로 리다이렉트합니다.
 */
public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        
        // Accept 헤더로 JSON 요청 여부 판단
        String acceptHeader = request.getHeader("Accept");
        boolean isJsonRequest = acceptHeader != null && acceptHeader.contains("application/json");
        
        // Content-Type 헤더로도 확인
        String contentType = request.getHeader("Content-Type");
        if (contentType != null && contentType.contains("application/json")) {
            isJsonRequest = true;
        }
        
        if (isJsonRequest) {
            // JSON 응답 반환
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "로그아웃되었습니다.");
            
            objectMapper.writeValue(response.getWriter(), result);
        } else {
            // 일반 폼 제출인 경우 로그인 페이지로 리다이렉트
            response.sendRedirect("/auth/login.html");
        }
    }
}


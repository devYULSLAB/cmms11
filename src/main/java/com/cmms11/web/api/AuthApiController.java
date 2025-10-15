package com.cmms11.web.api;

import com.cmms11.domain.member.MemberAuthResponse;
import com.cmms11.domain.member.MemberAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

/**
 * 이름: AuthApiController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 인증 관련 REST API 제공
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final MemberAuthService memberAuthService;

    public AuthApiController(MemberAuthService memberAuthService) {
        this.memberAuthService = memberAuthService;
    }

    /**
     * 현재 인증된 사용자 정보 조회
     */
    @GetMapping("/me")
    public MemberAuthResponse me(Authentication authentication) {
        return memberAuthService.getAuthenticatedMember(authentication);
    }

    /**
     * 로그아웃 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
        Authentication authentication,
        HttpServletRequest request
    ) {
        try {
            // 표준화된 성공 응답
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Logged out successfully",
                "timestamp", Instant.now().toString()
            );
            
            // 기본 로깅 (확장성 고려)
            if (authentication != null && authentication.isAuthenticated()) {
                // 로그 레벨에 따라 적절한 로깅 (현재는 System.out 사용)
                System.out.println("User " + authentication.getName() + 
                    " logged out from IP: " + getClientIpAddress(request));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Spring Security가 처리하므로 기본 성공 응답 반환
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Logged out successfully"
            ));
        }
    }
    
    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


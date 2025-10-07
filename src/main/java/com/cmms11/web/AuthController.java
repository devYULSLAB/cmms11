package com.cmms11.web;

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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberAuthService memberAuthService;

    public AuthController(MemberAuthService memberAuthService) {
        this.memberAuthService = memberAuthService;
    }

    @GetMapping("/me")
    public MemberAuthResponse me(Authentication authentication) {
        return memberAuthService.getAuthenticatedMember(authentication);
    }

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
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


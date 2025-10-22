package com.cmms11.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 이름: RequestLoggingFilter
 * 작성자: codex
 * 작성일: 2025-08-20
 * 수정일: 2025-10-17
 * 프로그램 개요: HTTP 요청/응답 메타데이터를 로깅하여 감사 추적을 지원하는 필터.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    // 민감 정보 필드 (로그에서 제외)
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
        "password", "passwd", "pwd", "token", "secret", "apikey", "api_key"
    );

    // 로깅 제외할 경로 (정적 리소스 등)
    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/assets/", "/static/", "/favicon.ico", "/webjars/"
    );

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 정적 리소스는 로깅 제외
        String uri = request.getRequestURI();
        if (shouldSkipLogging(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        long started = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - started;
            logRequest(request, response, elapsed);
        }
    }

    /**
     * 요청 로깅
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long elapsed) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        // 사용자 정보 가져오기
        String username = getUsername();
        String companyId = getCompanyId();
        String clientIp = getClientIP(request);
        
        // 파라미터 정보 (선택적)
        String params = getRequestParams(request);
        
        // 로그 출력
        if (params != null && !params.isEmpty()) {
            log.info("[REQ] user={} company={} ip={} {} {} {} status={} duration={}ms", 
                username, companyId, clientIp, method, uri, params, status, elapsed);
        } else {
            log.info("[REQ] user={} company={} ip={} {} {} status={} duration={}ms", 
                username, companyId, clientIp, method, uri, status, elapsed);
        }
        
        // 에러 상태는 WARN 레벨로
        if (status >= 400) {
            log.warn("[ERR] user={} company={} {} {} returned {}", 
                username, companyId, method, uri, status);
        }
    }

    /**
     * SecurityContext에서 사용자 이름 가져오기
     */
    private String getUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                // MemberUserDetailsService.getCurrentMemberId() 사용
                return com.cmms11.security.MemberUserDetailsService.getCurrentMemberId();
            }
        } catch (Exception e) {
            // 무시
        }
        return "anonymous";
    }

    /**
     * SecurityContext에서 회사 ID 가져오기
     */
    private String getCompanyId() {
        try {
            // MemberUserDetailsService.getCurrentUserCompanyId() 사용
            return com.cmms11.security.MemberUserDetailsService.getCurrentUserCompanyId();
        } catch (Exception e) {
            // 무시
        }
        return "-";
    }

    /**
     * 클라이언트 IP 주소 가져오기 (프록시 고려)
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째만
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * 요청 파라미터 문자열 생성 (민감 정보 필터링)
     */
    private String getRequestParams(HttpServletRequest request) {
        String method = request.getMethod();
        
        // GET 요청은 쿼리 스트링
        if ("GET".equals(method)) {
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                return "?" + filterSensitiveParams(queryString);
            }
        }
        
        // POST/PUT/DELETE는 주요 파라미터만
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            StringBuilder params = new StringBuilder();
            Enumeration<String> paramNames = request.getParameterNames();
            
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (!isSensitiveParam(paramName)) {
                    String[] values = request.getParameterValues(paramName);
                    if (values != null && values.length > 0) {
                        if (params.length() > 0) {
                            params.append("&");
                        }
                        params.append(paramName).append("=").append(values[0]);
                        
                        // 너무 길면 자르기
                        if (params.length() > 200) {
                            params.append("...");
                            break;
                        }
                    }
                }
            }
            
            if (params.length() > 0) {
                return "params={" + params + "}";
            }
        }
        
        return null;
    }

    /**
     * 민감한 파라미터인지 확인
     */
    private boolean isSensitiveParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lowerName = paramName.toLowerCase();
        return SENSITIVE_PARAMS.stream().anyMatch(lowerName::contains);
    }

    /**
     * 쿼리 스트링에서 민감 정보 필터링
     */
    private String filterSensitiveParams(String queryString) {
        if (queryString == null) {
            return "";
        }
        String[] params = queryString.split("&");
        StringBuilder filtered = new StringBuilder();
        
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length > 0) {
                if (filtered.length() > 0) {
                    filtered.append("&");
                }
                String key = keyValue[0];
                if (isSensitiveParam(key)) {
                    filtered.append(key).append("=***");
                } else {
                    filtered.append(param);
                }
            }
        }
        
        return filtered.toString();
    }

    /**
     * 로깅을 건너뛸 경로인지 확인
     */
    private boolean shouldSkipLogging(String uri) {
        if (uri == null) {
            return false;
        }
        return EXCLUDED_PATHS.stream().anyMatch(uri::startsWith);
    }
}


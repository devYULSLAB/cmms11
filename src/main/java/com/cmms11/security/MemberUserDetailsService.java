package com.cmms11.security;

import com.cmms11.domain.member.Member;
import com.cmms11.domain.member.MemberRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberUserDetailsService implements UserDetailsService {
    public static final String DEFAULT_COMPANY = "CHROK";

    private final MemberRepository memberRepository;

    public MemberUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // ⭐ 입력값 기본 검증
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }
        
        String companyId = DEFAULT_COMPANY;
        String memberId = username;
        
        if (username.contains(":")) {
            String[] parts = username.split(":", 2);
            
            // ⭐ 검증: 정확히 2개 부분이어야 하고 비어있지 않아야 함
            if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                throw new UsernameNotFoundException("Invalid credentials");
            }
            
            companyId = parts[0].trim();
            memberId = parts[1].trim();
            
            // ⭐ 검증: memberId에 추가 콜론 차단 (다중 : 우회 방지)
            if (memberId.contains(":")) {
                throw new UsernameNotFoundException("Invalid credentials");
            }
        }

        // DB 조회 (통일된 에러 메시지)
        Member m = memberRepository.findByIdCompanyIdAndIdMemberId(companyId, memberId)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        boolean enabled = (m.getDeleteMark() == null || !"Y".equalsIgnoreCase(m.getDeleteMark()));
        List<GrantedAuthority> auths = new ArrayList<>();
        if ("admin".equalsIgnoreCase(memberId)) {
            auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return new User(memberId, m.getPasswordHash(), enabled, true, true, true, auths);
    }
    
    /**
     * 현재 인증된 사용자의 회사코드를 반환
     * loadUserByUsername에서 이미 파싱한 정보를 재사용
     * @return 회사코드 (예: C0001, C0002 등)
     */
    public static String getCurrentUserCompanyId() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return DEFAULT_COMPANY;
            }
            
            String username = authentication.getName();
            
            // loadUserByUsername에서 이미 파싱한 로직과 동일
            if (username != null && username.contains(":")) {
                String[] parts = username.split(":", 2);
                return parts[0]; // 회사코드 반환
            }
            
            return DEFAULT_COMPANY;
            
        } catch (Exception e) {
            return DEFAULT_COMPANY;
        }
    }
    
    /**
     * 현재 인증된 사용자의 멤버 ID를 반환
     * @return 멤버 ID (예: admin, user001 등)
     */
    public static String getCurrentMemberId() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return "system";
            }
            
            String username = authentication.getName();
            
            // loadUserByUsername에서 이미 파싱한 로직과 동일
            if (username != null && username.contains(":")) {
                String[] parts = username.split(":", 2);
                return parts[1]; // 멤버 ID 반환
            }
            
            return username != null ? username : "system";
            
        } catch (Exception e) {
            return "system";
        }
    }
}


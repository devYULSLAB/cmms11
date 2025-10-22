package com.cmms11.domain.member;

import com.cmms11.common.error.UnauthorizedException;
import com.cmms11.security.MemberUserDetailsService;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;

    public MemberAuthService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberAuthResponse getAuthenticatedMember(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Authentication is required");
        }

        // MemberUserDetailsService의 메서드를 사용하여 파싱
        String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
        String memberId = MemberUserDetailsService.getCurrentMemberId();

        Member member = memberRepository
                .findByIdCompanyIdAndIdMemberId(companyId, memberId)
                .orElse(null);

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return MemberAuthResponse.of(memberId, companyId, roles, member);
    }
}

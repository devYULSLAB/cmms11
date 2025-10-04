package com.cmms11.web;

import com.cmms11.config.AppConfig;
import com.cmms11.domain.member.Member;
import com.cmms11.domain.member.MemberService;
import com.cmms11.domain.dept.DeptService;
import com.cmms11.domain.site.SiteService;
import com.cmms11.security.MemberUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 이름: LayoutController
 * 작성자: codex
 * 작성일: 2025-01-27
 * 수정일:
 * 프로그램 개요: 레이아웃 템플릿을 제공하는 컨트롤러.
 */
@Controller
public class LayoutController {

    private final MemberService memberService;
    private final DeptService deptService;
    private final SiteService siteService;
    private final AppConfig.FileStorageConfig fileStorageConfig;
    private final Environment environment;

    @Autowired
    public LayoutController(MemberService memberService, DeptService deptService, SiteService siteService,
                          AppConfig.FileStorageConfig fileStorageConfig, Environment environment) {
        this.memberService = memberService;
        this.deptService = deptService;
        this.siteService = siteService;
        this.fileStorageConfig = fileStorageConfig;
        this.environment = environment;
    }

    /**
     * 조직도 선택 팝업 페이지
     */
    @GetMapping("/common/org-picker")
    public String orgPicker() {
        return "common/org-picker";
    }

    /**
     * 설비 선택 팝업 페이지
     */
    @GetMapping("/common/plant-picker")
    public String plantPicker() {
        return "common/plant-picker";
    }

    /**
     * 내 정보 수정 팝업 페이지
     */
    @GetMapping("/common/profile-edit")
    public String profileEdit(Model model) {
        try {
            String memberId = MemberUserDetailsService.getCurrentMemberId();
            Member member = memberService.get(memberId);
            model.addAttribute("member", member);
            model.addAttribute("depts", deptService.list(null, Pageable.unpaged()).getContent());
            model.addAttribute("sites", siteService.list(null, Pageable.unpaged()).getContent());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "사용자 정보를 불러올 수 없습니다: " + e.getMessage());
        }
        return "common/profile-edit";
    }

    /**
     * 내 정보 수정 처리
     */
    @PostMapping("/common/profile-edit")
    public String profileEditSave(@ModelAttribute Member memberForm,
                                   @RequestParam(required = false) String password,
                                   RedirectAttributes redirectAttributes) {
        try {
            String memberId = MemberUserDetailsService.getCurrentMemberId();
            Member existing = memberService.get(memberId);
            
            // 수정 가능한 필드만 업데이트
            existing.setName(memberForm.getName());
            existing.setEmail(memberForm.getEmail());
            existing.setPhone(memberForm.getPhone());
            existing.setDeptId(memberForm.getDeptId());
            existing.setSiteId(memberForm.getSiteId());
            existing.setPosition(memberForm.getPosition());
            existing.setTitle(memberForm.getTitle());
            existing.setNote(memberForm.getNote());
            
            memberService.update(existing, password, null);
            
            redirectAttributes.addFlashAttribute("successMessage", "정보가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "정보 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/common/profile-edit";
    }

    /**
     * 현재 활성화된 프로파일 정보를 반환
     */
    private String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles[0] : "default";
    }

    /**
     * 기본 레이아웃 페이지를 제공합니다.
     * 
     * @param content SPA에서 로드할 콘텐츠 URL
     * @param authentication 인증된 사용자 정보
     * @param model 뷰 모델
     * @return 레이아웃 템플릿 이름
     */
    @GetMapping("/layout/defaultLayout.html")
    public String defaultLayout(
            @RequestParam(required = false) String content,
            Authentication authentication,
            Model model) {
        
        // 기본 콘텐츠 설정
        if (content == null || content.trim().isEmpty()) {
            content = "/plant/list.html";
        }
        model.addAttribute("content", content);
        
        // 인증된 사용자 정보 추가
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            model.addAttribute("username", username);
            model.addAttribute("isAuthenticated", true);
            
            // 사용자명에서 회사코드와 멤버ID 분리
            String companyId = MemberUserDetailsService.getCurrentUserCompanyId();
            String memberId = MemberUserDetailsService.getCurrentMemberId();
            model.addAttribute("companyId", companyId);
            model.addAttribute("memberId", memberId);
            
            // 사용자 상세 정보 조회
            try {
                Member member = memberService.get(memberId);
                model.addAttribute("currentMember", member);
                model.addAttribute("deptId", member.getDeptId());
                model.addAttribute("siteId", member.getSiteId());
            } catch (Exception e) {
                // 사용자 정보 조회 실패 시 기본값
                model.addAttribute("deptId", "-");
                model.addAttribute("siteId", "-");
            }
        } else {
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("username", "게스트");
        }
        
        // 파일 업로드 설정을 JavaScript에서 사용할 수 있도록 모델에 추가
        model.addAttribute("fileUploadConfig", new FileUploadConfigDto(
            fileStorageConfig.getMaxSize(),
            fileStorageConfig.getAllowedExtensionsArray(),
            fileStorageConfig.getMaxSizeFormatted(),
            getActiveProfile() // 현재 활성화된 프로파일 정보 추가
        ));
        
        return "layout/defaultLayout";
    }

    /**
     * 파일 업로드 설정을 JavaScript로 전달하기 위한 DTO 클래스
     */
    public static class FileUploadConfigDto {
        private final long maxSize;
        private final String[] allowedExtensions;
        private final String maxSizeFormatted;
        private final String profile;

        public FileUploadConfigDto(long maxSize, String[] allowedExtensions, String maxSizeFormatted, String profile) {
            this.maxSize = maxSize;
            this.allowedExtensions = allowedExtensions;
            this.maxSizeFormatted = maxSizeFormatted;
            this.profile = profile;
        }

        public long getMaxSize() { return maxSize; }
        public String[] getAllowedExtensions() { return allowedExtensions; }
        public String getMaxSizeFormatted() { return maxSizeFormatted; }
        public String getProfile() { return profile; }
    }
}

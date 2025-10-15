package com.cmms11.web;

import com.cmms11.domain.company.Company;
import com.cmms11.domain.company.CompanyRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 이름: LoginController
 * 작성자: codex
 * 작성일: 2025-10-13
 * 프로그램 개요: 로그인 페이지 제공 및 회사 목록 동적 로딩
 */
@Controller
public class LoginController {

    private final CompanyRepository companyRepository;

    public LoginController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * 로그인 페이지 제공
     * 활성 회사 목록을 동적으로 조회하여 모델에 추가
     * 쿠키에서 마지막 로그인 정보 읽어서 폼 초기화
     */
    @GetMapping("/auth/login.html")
    public String loginPage(
        jakarta.servlet.http.HttpServletRequest request,
        Model model
    ) {
        // ⭐ 활성 회사 목록 조회 (delete_mark='N')
        List<Company> companies = companyRepository.findAll(Sort.by(Sort.Direction.ASC, "companyId"))
            .stream()
            .filter(c -> !"Y".equals(c.getDeleteMark()))
            .toList();
        
        model.addAttribute("companies", companies);
        
        // ✨ 쿠키에서 마지막 로그인 정보 읽기
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        String lastCompanyId = null;
        String lastUsername = null;
        boolean rememberLogin = false;
        
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("cmms_company_id".equals(cookie.getName())) {
                    lastCompanyId = cookie.getValue();
                } else if ("cmms_username".equals(cookie.getName())) {
                    lastUsername = cookie.getValue();
                } else if ("cmms_remember".equals(cookie.getName())) {
                    rememberLogin = "true".equals(cookie.getValue());
                }
            }
        }
        
        model.addAttribute("lastCompanyId", lastCompanyId);
        model.addAttribute("lastUsername", lastUsername);
        model.addAttribute("rememberLogin", rememberLogin);
        
        return "auth/login";
    }
}


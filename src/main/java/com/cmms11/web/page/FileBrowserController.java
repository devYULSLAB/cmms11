package com.cmms11.web.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * FileBrowser 페이지 컨트롤러
 */
@Controller
@RequestMapping("/filebrowser")
public class FileBrowserController {
    
    /**
     * 파일 브라우저 메인 페이지
     * GET /filebrowser
     */
    @GetMapping
    public String browser(Model model) {
        model.addAttribute("pageTitle", "파일 관리");
        model.addAttribute("content", "/filebrowser/browser");
        return "layout/defaultLayout";
    }
}


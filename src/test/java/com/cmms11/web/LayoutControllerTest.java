package com.cmms11.web;

import com.cmms11.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = LayoutController.class)
class LayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppConfig.FileStorageConfig fileStorageConfig;

    @Test
    void defaultLayout_파일업로드설정전달() throws Exception {
        mockMvc.perform(get("/layout/defaultLayout")
                .param("content", "/workorder/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/defaultLayout"))
                .andExpect(model().attributeExists("content"))
                .andExpect(model().attributeExists("fileUploadConfig"));
    }

    @Test
    void defaultLayout_다양한콘텐츠패턴() throws Exception {
        // 다양한 콘텐츠 패턴 테스트
        String[] contents = {
            "/workorder/list",
            "/plant/detail?id=EQP001", 
            "/member/form",
            "/inventory/list?page=0&size=10"
        };

        for (String content : contents) {
            mockMvc.perform(get("/layout/defaultLayout")
                    .param("content", content))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout/defaultLayout"))
                    .andExpect(model().attribute("content", content));
        }
    }
}

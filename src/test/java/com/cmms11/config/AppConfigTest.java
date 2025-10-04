package com.cmms11.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "app.file-storage.max-size=2097152",
    "app.file-storage.allowed-extensions=txt,pdf,jpg"
})
class AppConfigTest {

    @Autowired
    private AppConfig.FileStorageConfig fileStorageConfig;

    @Test
    void fileStorageConfig_기본설정로드() {
        assertThat(fileStorageConfig.getMaxSize()).isEqualTo(2097152L);
        assertThat(fileStorageConfig.getAllowedExtensions()).isEqualTo("txt,pdf,jpg");
        assertThat(fileStorageConfig.getAllowedExtensionsArray()).containsExactly("txt", "pdf", "jpg");
        assertThat(fileStorageConfig.getMaxSizeFormatted()).contains("MB");
    }

    @Test 
    void fileStorageConfig_커스텀설정로드() {
        assertThat(fileStorageConfig.getMaxSize()).isEqualTo(2097152L);
        assertThat(fileStorageConfig.getAllowedExtensionsArray()).hasSize(3)
            .contains("txt", "pdf", "jpg");
    }
}

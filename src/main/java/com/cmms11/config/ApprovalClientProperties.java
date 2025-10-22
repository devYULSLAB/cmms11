package com.cmms11.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Approval REST API 호출 관련 설정.
 */
@ConfigurationProperties(prefix = "app.approval")
public class ApprovalClientProperties {

    private String baseUrl = "http://localhost:8080";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}

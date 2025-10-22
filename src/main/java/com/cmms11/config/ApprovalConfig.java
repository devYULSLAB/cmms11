package com.cmms11.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 결재/Webhook 관련 설정 바인딩.
 */
@Configuration
@EnableConfigurationProperties({ApprovalWebhookProperties.class, ApprovalClientProperties.class})
public class ApprovalConfig {
}

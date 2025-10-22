package com.cmms11.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 결재 Webhook 관련 설정.
 */
@ConfigurationProperties(prefix = "app.webhook")
public class ApprovalWebhookProperties {

    private String callbackBase = "http://localhost:8080";
    private final Security security = new Security();
    private final Retry retry = new Retry();
    private final Scheduler scheduler = new Scheduler();

    public String getCallbackBase() {
        return callbackBase;
    }

    public void setCallbackBase(String callbackBase) {
        this.callbackBase = callbackBase;
    }

    public Security getSecurity() {
        return security;
    }

    public Retry getRetry() {
        return retry;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static class Security {
        private String secretKey = "cmms11_dev_secret_key";

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }

    public static class Retry {
        private int maxAttempts = 5;
        private long backoffMillis = 5000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMillis() {
            return backoffMillis;
        }

        public void setBackoffMillis(long backoffMillis) {
            this.backoffMillis = backoffMillis;
        }
    }

    public static class Scheduler {
        private long delayMillis = 5000;

        public long getDelayMillis() {
            return delayMillis;
        }

        public void setDelayMillis(long delayMillis) {
            this.delayMillis = delayMillis;
        }
    }
}

package com.cmms11.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 설정 클래스
 * YML 파일의 커스텀 설정을 Java 객체로 바인딩
 * 
 * @author codex
 */
@Configuration
@EnableConfigurationProperties(AppConfig.FileStorageConfig.class)
public class AppConfig {

    /**
     * 파일 저장소 설정 Properties 클래스
     */
    @ConfigurationProperties(prefix = "app.file-storage")
    public static class FileStorageConfig {
        private String location = "storage/uploads";
        private long maxSize = 10485760L; // 10MB 기본값
        private String allowedExtensions = "jpg,jpeg,png,pdf,doc,docx,xls,xlsx,hwp,hwpx,zip,txt";

        // Getters and Setters
        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public String getAllowedExtensions() {
            return allowedExtensions;
        }

        public void setAllowedExtensions(String allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        /**
         * 허용된 확장자 목록을 배열로 반환
         */
        public String[] getAllowedExtensionsArray() {
            return allowedExtensions.split(",");
        }

        /**
         * 파일 크기를 사람이 읽기 쉬운 형태로 변환
         */
        public String getMaxSizeFormatted() {
            double sizeInMB = maxSize / (1024.0 * 1024.0);
            return String.format("%.1fMB", sizeInMB);
        }
    }

    /**
     * AWS S3 설정 Properties 클래스
     */
    @ConfigurationProperties(prefix = "aws.s3")
    public static class S3Config {
        private String bucketName;
        private boolean versioning = false;
        private boolean preserveOriginalFilename = true;
        private long maxSize = 10485760L;
        private String allowedExtensions = "jpg,jpeg,png,pdf,doc,docx,xls,xlsx,hwp,hwpx,zip,txt";

        // Getters and Setters
        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public boolean isVersioning() {
            return versioning;
        }

        public void setVersioning(boolean versioning) {
            this.versioning = versioning;
        }

        public boolean isPreserveOriginalFilename() {
            return preserveOriginalFilename;
        }

        public void setPreserveOriginalFilename(boolean preserveOriginalFilename) {
            this.preserveOriginalFilename = preserveOriginalFilename;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(long maxSize) {
            this.maxSize = maxSize;
        }

        public String getAllowedExtensions() {
                return allowedExtensions;
        }

        public void setAllowedExtensions(String allowedExtensions) {
            this.allowedExtensions = allowedExtensions;
        }

        public String[] getAllowedExtensionsArray() {
            return allowedExtensions.split(",");
        }

        public String getMaxSizeFormatted() {
            double sizeInMB = maxSize / (1024.0 * 1024.0);
            return String.format("%.1fMB", sizeInMB);
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "aws.s3")
    public S3Config s3Config() {
        return new S3Config();
    }
}

package com.cmms11.approval.client;

import com.cmms11.approval.ApprovalRequest;
import com.cmms11.approval.ApprovalResponse;
import com.cmms11.config.ApprovalClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Approval REST API 연동 클라이언트.
 */
@Component
public class ApprovalClient {

    private static final Logger log = LoggerFactory.getLogger(ApprovalClient.class);

    private final RestTemplate restTemplate;
    private final ApprovalClientProperties properties;

    public ApprovalClient(RestTemplateBuilder restTemplateBuilder, ApprovalClientProperties properties) {
        this.restTemplate = restTemplateBuilder.build();
        this.properties = properties;
    }

    public ApprovalResponse submitApproval(ApprovalRequest request) {
        String url = properties.getBaseUrl();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += "api/approvals";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ApprovalRequest> httpEntity = new HttpEntity<>(request, headers);
        ResponseEntity<ApprovalResponse> response = restTemplate.postForEntity(url, httpEntity, ApprovalResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }

        log.error("Approval submission failed: status={}, body={}", response.getStatusCode(), response.getBody());
        throw new IllegalStateException("Approval submission failed: " + response.getStatusCode());
    }
}

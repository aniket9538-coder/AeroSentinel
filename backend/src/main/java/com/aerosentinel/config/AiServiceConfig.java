package com.aerosentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceConfig {

    @Value("${app.ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${app.ai-service.timeout-ms:10000}")
    private int timeoutMs;

    public String getAiServiceUrl() {
        return aiServiceUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }
}

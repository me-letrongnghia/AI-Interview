package com.capstone.ai_interview_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration cho WebClient để gọi external APIs
 * Sử dụng cho GenQ service và OpenRouter service
 */
@Configuration
public class WebClientConfig {
    
    /**
     * WebClient bean cho GenQ service và OpenRouter service
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB buffer
                .build();
    }
}

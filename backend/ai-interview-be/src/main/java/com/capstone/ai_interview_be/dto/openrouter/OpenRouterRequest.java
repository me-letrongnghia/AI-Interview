package com.capstone.ai_interview_be.dto.openrouter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenRouterRequest {
    private String model;
    private List<Message> messages;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens = 500;
    
    private Double temperature = 0.7;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role; // "system", "user", "assistant"
        private String content;
    }
}

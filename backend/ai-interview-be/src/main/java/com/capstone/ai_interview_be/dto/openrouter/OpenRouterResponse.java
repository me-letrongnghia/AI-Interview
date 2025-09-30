package com.capstone.ai_interview_be.dto.openrouter;

import lombok.Data;
import java.util.List;

@Data
public class OpenRouterResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    
    @Data
    public static class Choice {
        private int index;
        private Message message;
        private String finishReason;
        
        @Data
        public static class Message {
            private String role;
            private String content;
        }
    }
    
    @Data
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}
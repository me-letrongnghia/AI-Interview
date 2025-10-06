package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessAnswerResponse {
    private Long answerId;
    private String feedback;
    private NextQuestion nextQuestion;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NextQuestion {
        private Long questionId;
        private String content;
    }
}
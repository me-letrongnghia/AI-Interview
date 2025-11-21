package com.capstone.ai_interview_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateOverallFeedbackRequest {
    
    /**
     * List of Q&A pairs from the interview session
     */
    private List<ConversationQA> conversation;
    
    /**
     * Job role (e.g., "Java Backend Developer")
     */
    private String role;
    
    /**
     * Experience level (e.g., "Mid-level", "Senior")
     */
    private String seniority;
    
    /**
     * List of relevant skills (e.g., ["Java", "Spring Boot"])
     */
    private List<String> skills;
    
    /**
     * Q&A pair with scores and feedback
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationQA {
        /**
         * Question sequence number
         */
        private Integer sequenceNumber;
        
        /**
         * Interview question
         */
        private String question;
        
        /**
         * Candidate's answer
         */
        private String answer;
        
        /**
         * Evaluation scores for this answer
         * Keys: correctness, coverage, depth, clarity, practicality, final
         */
        private Map<String, Double> scores;
        
        /**
         * Feedback points for this answer
         */
        private List<String> feedback;
    }
}

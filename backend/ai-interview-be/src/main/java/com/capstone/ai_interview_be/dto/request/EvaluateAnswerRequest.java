package com.capstone.ai_interview_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for Judge AI service to evaluate interview answers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateAnswerRequest {
    
    private String question;
    
    private String answer;
    
    private String role;
    
    private String level;
    
    private String competency;
    
    private List<String> skills;
    
    private Map<String, Double> customWeights;
}

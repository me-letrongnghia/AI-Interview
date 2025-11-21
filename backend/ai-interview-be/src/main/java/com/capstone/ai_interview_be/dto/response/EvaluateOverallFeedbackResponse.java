package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateOverallFeedbackResponse {
    
    /**
     * Overall performance rating
     * Values: EXCELLENT, GOOD, AVERAGE, BELOW AVERAGE, POOR
     */
    private String overview;
    
    /**
     * Comprehensive assessment paragraph (4-6 sentences)
     */
    private String assessment;
    
    /**
     * List of key strengths (2-5 items)
     */
    private List<String> strengths;
    
    /**
     * List of areas needing improvement (2-4 items)
     */
    private List<String> weaknesses;
    
    /**
     * Actionable recommendations paragraph
     */
    private String recommendations;
    
    /**
     * Time taken to generate feedback (seconds)
     */
    private Double generationTime;
}

package com.capstone.ai_interview_be.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Multitask Judge EVALUATE task (API v2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskEvaluateResponse {
    
    private Integer relevance;      // 0-10
    
    private Integer completeness;   // 0-10
    
    private Integer accuracy;       // 0-10
    
    private Integer clarity;        // 0-10
    
    private Integer overall;        // 0-10
    
    private String feedback;
    
    @JsonProperty("improved_answer")
    private String improvedAnswer;  // Gợi ý câu trả lời cải thiện
    
    @JsonProperty("generation_time")
    private Double generationTime;
    
    @JsonProperty("model_used")
    private String modelUsed;
}

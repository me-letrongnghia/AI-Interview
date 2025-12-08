package com.capstone.ai_interview_be.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Multitask Judge GENERATE task (API v2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskGenerateResponse {
    
    private String question;
    
    @JsonProperty("question_type")
    private String questionType;    // follow_up, clarification, deep_dive
    
    private String difficulty;      // easy, medium, hard
    
    @JsonProperty("generation_time")
    private Double generationTime;
    
    @JsonProperty("model_used")
    private String modelUsed;
}

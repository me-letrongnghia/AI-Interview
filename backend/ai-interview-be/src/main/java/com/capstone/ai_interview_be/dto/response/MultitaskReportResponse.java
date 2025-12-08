package com.capstone.ai_interview_be.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Multitask Judge REPORT task (API v2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskReportResponse {
    
    @JsonProperty("overall_assessment")
    private String overallAssessment;
    
    private List<String> strengths;
    
    private List<String> weaknesses;
    
    private List<String> recommendations;
    
    private Integer score;          // 0-100
    
    @JsonProperty("generation_time")
    private Double generationTime;
    
    @JsonProperty("model_used")
    private String modelUsed;
}

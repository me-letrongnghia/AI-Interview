package com.capstone.ai_interview_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for Multitask Judge GENERATE task (API v2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskGenerateRequest {
    
    private String question;
    
    private String answer;
    
    @JsonProperty("interview_history")
    private List<Map<String, String>> interviewHistory;
    
    @JsonProperty("job_domain")
    private String jobDomain;
    
    @Builder.Default
    private String difficulty = "medium";
    
    @Builder.Default
    private Double temperature = 0.7;
}

package com.capstone.ai_interview_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskEvaluateRequest {
    
    private String question;
    
    private String answer;
    
    private String context;
    
    @JsonProperty("job_domain")
    private String jobDomain;
    
    @Builder.Default
    private String level = "mid-level";
    
    @Builder.Default
    private Double temperature = 0.3;
}

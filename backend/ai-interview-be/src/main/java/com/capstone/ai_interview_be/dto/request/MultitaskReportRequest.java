package com.capstone.ai_interview_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MultitaskReportRequest {
    
    @JsonProperty("interview_history")
    private List<Map<String, String>> interviewHistory;
    
    @JsonProperty("job_domain")
    private String jobDomain;
    
    @Builder.Default
    private String level = "mid-level";
    
    private List<String> skills;
    
    @JsonProperty("candidate_info")
    private String candidateInfo;
    
    @Builder.Default
    private Double temperature = 0.5;
}

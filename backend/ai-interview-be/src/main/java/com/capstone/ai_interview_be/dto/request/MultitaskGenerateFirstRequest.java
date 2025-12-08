package com.capstone.ai_interview_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for Multitask Judge GENERATE_FIRST task (API v2)
 * Used to generate the first interview question based on role/skills
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskGenerateFirstRequest {
    
    private String role;
    
    @Builder.Default
    private List<String> skills = List.of();
    
    @Builder.Default
    private String level = "mid-level";
    
    @Builder.Default
    private String language = "English";
    
    @JsonProperty("cv_context")
    private String cvContext;
    
    @JsonProperty("jd_context")
    private String jdContext;
    
    @Builder.Default
    private Double temperature = 0.7;
}

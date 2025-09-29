package com.capstone.ai_interview_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewSessionRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String domain;
    
    private String level;
    
    private Long userId;
}
package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewSessionResponse {
    private Long sessionId;
    private String message;
    
    public CreateInterviewSessionResponse(Long sessionId) {
        this.sessionId = sessionId;
        this.message = "Interview session created successfully";
    }
}
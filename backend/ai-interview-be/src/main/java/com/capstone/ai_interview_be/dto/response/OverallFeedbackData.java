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
public class OverallFeedbackData {
    
    private String overview;
    
    private String assessment;

    private List<String> strengths;
    
    private List<String> weaknesses;
    
    private String recommendations;
}

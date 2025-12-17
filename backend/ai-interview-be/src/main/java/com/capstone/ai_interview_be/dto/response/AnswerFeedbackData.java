package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFeedbackData {

    private String feedback;

    private String sampleAnswer;

    // Scores from AI evaluation (0-10 scale from model)
    private Integer relevance;
    private Integer completeness;
    private Integer accuracy;
    private Integer clarity;
    private Integer overall;
}

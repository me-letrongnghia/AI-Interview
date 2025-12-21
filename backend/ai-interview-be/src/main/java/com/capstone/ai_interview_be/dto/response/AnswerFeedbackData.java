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

    // Scores from AI response
    private Integer relevance; // 0-10
    private Integer completeness; // 0-10
    private Integer accuracy; // 0-10
    private Integer clarity; // 0-10
    private Integer overall; // 0-10
}

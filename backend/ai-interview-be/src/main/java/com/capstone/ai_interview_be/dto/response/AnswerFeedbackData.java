package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerFeedbackData {

    private Double score;

    private String feedback;

    private String sampleAnswer;

    private Map<String, Double> criteriaScores;
}

package com.capstone.ai_interview_be.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewFeedbackResponse {
    private Long sessionId;
    private SessionInfo sessionInfo;
    private OverallFeedback overallFeedback;
    private List<QuestionAnswerFeedback> conversationHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private String role;
        private String level;
        private List<String> skills;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String duration;
        private Integer totalQuestions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallFeedback {
        private Double overallScore;
        private String assessment;
        private List<String> strengths;
        private List<String> weaknesses;
        private String recommendations;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnswerFeedback {
        private Long questionId;
        private String question;
        private Long answerId;
        private String userAnswer;
        private Double score;
        private String feedback;
        private String sampleAnswer;
        private Map<String, Double> criteriaScores;
    }
}

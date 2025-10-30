package com.capstone.ai_interview_be.dto.websocket;

import lombok.Data;

@Data
public class FeedbackMessage {
    private String type; // "feedback", "question", "end", "interview_ended"
    private String feedback;
    private QuestionMessage nextQuestion;
    private String timestamp;
    private Boolean isComplete = false;
    private Long sessionId;
    private String redirectUrl;
}
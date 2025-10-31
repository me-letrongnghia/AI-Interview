package com.capstone.ai_interview_be.dto.websocket;

import lombok.Data;

@Data
public class AnswerMessage {
    private Long questionId;
    private String content;
    private String timestamp;
    private Boolean isLastAnswer; // Flag to indicate this is the last answer
}
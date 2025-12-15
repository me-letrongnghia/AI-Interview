package com.capstone.ai_interview_be.dto.message;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private String id;
    private String sessionId;
    private String type;
    private String content;
    private String questionId;
    private LocalDateTime timestamp;
    private Boolean isSystemMessage;
}

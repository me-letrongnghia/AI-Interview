package com.capstone.ai_interview_be.dto.message;

import java.util.List;

import com.capstone.ai_interview_be.model.InterviewQuestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryResponse {
    private Boolean success;
    private List<ChatMessageDTO> data;
    private InterviewQuestion question;
}

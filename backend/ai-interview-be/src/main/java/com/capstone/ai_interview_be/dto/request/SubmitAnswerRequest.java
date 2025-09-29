package com.capstone.ai_interview_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotNull(message = "Question ID is required")
    private Long questionId;
    
    @NotBlank(message = "Answer content is required")
    private String content;
}
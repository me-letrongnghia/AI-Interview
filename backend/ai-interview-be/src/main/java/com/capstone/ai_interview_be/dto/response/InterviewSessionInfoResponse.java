package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InterviewSessionInfoResponse {
    private Long id;
    private Long userId;
    private String role;
    private String level;
    private List<String> skill;
    private String language;
    private String title;
    private String description;
    private String source; // Custom, JD, CV
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer duration; // in minutes
    private Integer questionCount; // number of questions
    private Long feedbackId;
}

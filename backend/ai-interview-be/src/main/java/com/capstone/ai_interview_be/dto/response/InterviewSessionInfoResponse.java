package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewSessionInfoResponse {
    private Long id;
    private Long userId;
    private String role;
    private String level;
    private List<String> skill;
    private String language;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

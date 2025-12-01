package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopInterviewerResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private String position; // Latest position they interviewed for
    private Long interviews; // Total number of interviews
    private String totalDuration; // Total interview duration
    private String status; // User status (active/banned)
    private LocalDateTime lastInterviewDate; // Date of their most recent interview
}
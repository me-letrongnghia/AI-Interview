package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminInterviewResponse {
    private Long id;
    private String userName;
    private String userEmail;
    private String position;
    private String duration;
    private Double score;
    private String status;
    private LocalDateTime date;
}

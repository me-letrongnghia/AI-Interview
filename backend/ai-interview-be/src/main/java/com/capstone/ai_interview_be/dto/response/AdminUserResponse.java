package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String status;
    private Long interviews;
    private Double avgScore;
    private LocalDateTime joinDate;
}

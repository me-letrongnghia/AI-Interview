package com.capstone.ai_interview_be.dto.CreateInterviewSession;


import java.util.List;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewSessionRequest {

    @Column(name = "user_id")
    private Long userId;

    private String role;

    private String level;

    private List<String> skill;

    private String language;

    private String source;


}
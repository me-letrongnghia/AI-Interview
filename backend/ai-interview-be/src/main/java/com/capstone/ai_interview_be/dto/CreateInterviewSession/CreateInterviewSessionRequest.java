package com.capstone.ai_interview_be.dto.CreateInterviewSession;


import java.util.List;

import com.capstone.ai_interview_be.model.InterviewSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewSessionRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Level is required")
    private String level;

    private List<String> skill;

    private String language = "en";

    private InterviewSession.Source source;

    // CV và JD text gốc (optional) - để GenQ service tạo câu hỏi tốt hơn
    private String cvText;
    
    private String jdText;

}
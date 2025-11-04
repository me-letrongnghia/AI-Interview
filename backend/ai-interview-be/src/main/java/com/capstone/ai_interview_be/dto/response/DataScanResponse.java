package com.capstone.ai_interview_be.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataScanResponse {

    private String role;

    private String level;

    private List<String> skill;

    private String language = "en";
    
    // Raw extracted text from CV/JD for GenQ service
    private String extractedText;
    
    // Constructor without extractedText for backward compatibility
    public DataScanResponse(String role, String level, List<String> skill, String language) {
        this.role = role;
        this.level = level;
        this.skill = skill;
        this.language = language;
    }

}

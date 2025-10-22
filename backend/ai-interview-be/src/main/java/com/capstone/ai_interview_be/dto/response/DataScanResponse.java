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

    private String language;


}

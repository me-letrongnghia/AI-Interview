package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeeklyActivityResponse {
    private List<DailyActivity> activities;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyActivity {
        private String name;
        private Long interviews;
        private Long users;
    }
}

package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardStatsResponse {
    private Long totalUsers;
    private Long totalInterviews;
    private Long interviewsThisWeek;
    private Long activeToday;
}

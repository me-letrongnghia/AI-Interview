package com.capstone.ai_interview_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultitaskGenerateRequest {

    private String question;

    private String answer;

    @JsonProperty("interview_history")
    private List<Map<String, String>> interviewHistory;

    @JsonProperty("job_domain")
    private String jobDomain;

    @Builder.Default
    private String level = "mid-level";

    private List<String> skills;

    @JsonProperty("current_question_number")
    @Builder.Default
    private int currentQuestionNumber = 0;

    @JsonProperty("total_questions")
    @Builder.Default
    private int totalQuestions = 0;

    @Builder.Default
    private String language = "English";

    @JsonProperty("cv_context")
    private String cvContext;

    @JsonProperty("jd_context")
    private String jdContext;

    @Builder.Default
    private Double temperature = 0.7;
}

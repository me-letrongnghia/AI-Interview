package com.capstone.ai_interview_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO from Judge AI service after evaluating interview answers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateAnswerResponse {
    
    /**
     * Scores for each evaluation dimension plus final weighted score
     * Keys: correctness, coverage, depth, clarity, practicality, final
     */
    private Map<String, Double> scores;
    
    /**
     * List of specific feedback points (3-5 items)
     * e.g., ["Strong: Clear explanation", "Improve: Add examples", "Missing: Best practices"]
     */
    private List<String> feedback;
    
    /**
     * Improved version of the candidate's answer with technical details
     */
    private String improvedAnswer;
    
    /**
     * Time taken to generate evaluation (in seconds)
     */
    private Double generationTime;
}

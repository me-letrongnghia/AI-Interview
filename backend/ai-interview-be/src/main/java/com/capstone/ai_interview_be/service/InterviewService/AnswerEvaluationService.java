package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.EvaluateAnswerResponse;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.service.AIService.AIService;
import com.capstone.ai_interview_be.service.AIService.JudgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to evaluate interview answers using both Judge AI and Gemini
 * Combines detailed scoring from Judge with qualitative feedback from Gemini
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerEvaluationService {
    
    private final JudgeService judgeService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;
    
    @Value("${judge.service.enabled:true}")
    private boolean judgeEnabled;
    
    /**
     * Evaluate an interview answer with Judge AI and Gemini feedback
     * 
     * @param question The interview question
     * @param answer The candidate's answer
     * @param role Job role (e.g., "Java Backend Developer")
     * @param level Experience level (e.g., "Mid-level")
     * @param competency Main competency being tested (e.g., "Spring Boot")
     * @param skills List of relevant skills
     * @return AnswerFeedback entity with all evaluation data
     */
    public AnswerFeedback evaluateAnswer(
            Long answerId,
            String question,
            String answer,
            String role,
            String level,
            String competency,
            List<String> skills) {
        
        log.info("Evaluating answer {} for role: {}, level: {}, competency: {}", 
                answerId, role, level, competency);
        
        AnswerFeedback answerFeedback = new AnswerFeedback();
        answerFeedback.setAnswerId(answerId);
        
        // Try Judge AI evaluation first (if enabled and service healthy)
        if (judgeEnabled && judgeService.isServiceHealthy()) {
            try {
                log.info("Using Judge AI for answer evaluation");
                EvaluateAnswerResponse judgeResponse = judgeService.evaluateAnswer(
                        question, answer, role, level, competency, skills);
                
                // Store Judge evaluation data
                populateJudgeData(answerFeedback, judgeResponse);
                
                log.info("Judge AI evaluation completed - Final score: {}", 
                        judgeResponse.getScores().get("final"));
                
            } catch (Exception e) {
                log.error("Error getting Judge AI evaluation, will use Gemini only", e);
            }
        } else {
            log.info("Judge AI service not enabled or unavailable, using Gemini only");
        }
        
        // Get Gemini feedback (qualitative assessment)
        try {
            log.info("Getting Gemini feedback");
            AnswerFeedbackData geminiData = aiService.generateAnswerFeedback(
                    question, answer, role, level);
            
            // Store Gemini feedback
            answerFeedback.setFeedbackText(geminiData.getFeedback());
            answerFeedback.setSampleAnswer(geminiData.getSampleAnswer());
            
            // If Judge didn't provide improved answer, use Gemini's sample answer
            if (answerFeedback.getImprovedAnswer() == null) {
                answerFeedback.setImprovedAnswer(geminiData.getSampleAnswer());
            }
            
            log.info("Gemini feedback generated successfully");
            
        } catch (Exception e) {
            log.error("Error getting Gemini feedback", e);
            answerFeedback.setFeedbackText("Error generating feedback: " + e.getMessage());
        }
        
        return answerFeedback;
    }
    
    /**
     * Populate AnswerFeedback entity with Judge AI evaluation data
     */
    private void populateJudgeData(AnswerFeedback feedback, EvaluateAnswerResponse judgeResponse) {
        try {
            // Store scores as JSON
            String scoresJson = objectMapper.writeValueAsString(judgeResponse.getScores());
            feedback.setScoresJson(scoresJson);
            
            // Store individual scores for easy querying
            feedback.setScoreCorrectness(judgeResponse.getScores().get("correctness"));
            feedback.setScoreCoverage(judgeResponse.getScores().get("coverage"));
            feedback.setScoreDepth(judgeResponse.getScores().get("depth"));
            feedback.setScoreClarity(judgeResponse.getScores().get("clarity"));
            feedback.setScorePracticality(judgeResponse.getScores().get("practicality"));
            feedback.setScoreFinal(judgeResponse.getScores().get("final"));
            
            // Store improved answer from Judge
            feedback.setImprovedAnswer(judgeResponse.getImprovedAnswer());
            
            // Store generation time
            feedback.setGenerationTime(judgeResponse.getGenerationTime());
            
            // Format Judge feedback as bullet points
            if (judgeResponse.getFeedback() != null && !judgeResponse.getFeedback().isEmpty()) {
                String feedbackText = formatJudgeFeedback(judgeResponse);
                feedback.setFeedbackText(feedbackText);
            }
            
        } catch (Exception e) {
            log.error("Error populating Judge data", e);
        }
    }
    
    /**
     * Format Judge feedback into readable text with scores
     */
    private String formatJudgeFeedback(EvaluateAnswerResponse judgeResponse) {
        StringBuilder sb = new StringBuilder();
        
        // Overall score
        sb.append("Overall Score: ")
          .append(String.format("%.2f", judgeResponse.getScores().get("final") * 100))
          .append("%\n\n");
        
        // Detailed scores
        sb.append("Detailed Scores:\n");
        sb.append(String.format("• Correctness: %.2f%%\n", 
                judgeResponse.getScores().get("correctness") * 100));
        sb.append(String.format("• Coverage: %.2f%%\n", 
                judgeResponse.getScores().get("coverage") * 100));
        sb.append(String.format("• Depth: %.2f%%\n", 
                judgeResponse.getScores().get("depth") * 100));
        sb.append(String.format("• Clarity: %.2f%%\n", 
                judgeResponse.getScores().get("clarity") * 100));
        sb.append(String.format("• Practicality: %.2f%%\n\n", 
                judgeResponse.getScores().get("practicality") * 100));
        
        // Feedback points
        sb.append("Feedback:\n");
        for (String point : judgeResponse.getFeedback()) {
            sb.append("• ").append(point).append("\n");
        }
        
        return sb.toString();
    }
}

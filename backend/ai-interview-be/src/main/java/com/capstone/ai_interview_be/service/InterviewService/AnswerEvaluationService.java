package com.capstone.ai_interview_be.service.InterviewService;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.model.AnswerFeedback;
import com.capstone.ai_interview_be.service.AIService.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to evaluate interview answers using Multitask Judge AI (via AIService)
 * Provides detailed scoring and qualitative feedback
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerEvaluationService {
    
    private final AIService aiService;
    
    /**
     * Evaluate an interview answer with Multitask Judge AI
     * 
     * @param answerId The answer ID
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
        
        try {
            log.info("Using Multitask Judge AI for answer evaluation");
            
            // Get evaluation from AIService (which uses MultitaskJudgeService)
            AnswerFeedbackData feedbackData = aiService.generateAnswerFeedback(
                    question, answer, role, level);
            
            // Parse scores from feedback text if available
            parseAndSetScores(answerFeedback, feedbackData.getFeedback());
            
            // Store feedback and sample answer
            answerFeedback.setFeedbackText(feedbackData.getFeedback());
            answerFeedback.setSampleAnswer(feedbackData.getSampleAnswer());
            answerFeedback.setImprovedAnswer(feedbackData.getSampleAnswer());
            
            log.info("Multitask Judge evaluation completed for answer {}", answerId);
            
        } catch (Exception e) {
            log.error("Error evaluating answer {}: {}", answerId, e.getMessage());
            answerFeedback.setFeedbackText("Error generating feedback: " + e.getMessage());
        }
        
        return answerFeedback;
    }
    
    /**
     * Parse scores from feedback text and set them on AnswerFeedback
     * Expected format: "Overall Score: X.X/10\n...• Relevance: X/10\n..."
     */
    private void parseAndSetScores(AnswerFeedback feedback, String feedbackText) {
        if (feedbackText == null || feedbackText.isEmpty()) {
            setDefaultScores(feedback);
            return;
        }
        
        try {
            // Parse Overall Score
            if (feedbackText.contains("Overall Score:")) {
                String overallStr = extractScore(feedbackText, "Overall Score:");
                if (overallStr != null) {
                    double overall = Double.parseDouble(overallStr) / 10.0; // Convert to 0-1 scale
                    feedback.setScoreFinal(overall);
                }
            }
            
            // Parse individual scores (convert from 0-10 to 0-1)
            feedback.setScoreCorrectness(parseScoreFromText(feedbackText, "Accuracy:") / 10.0);
            feedback.setScoreCoverage(parseScoreFromText(feedbackText, "Completeness:") / 10.0);
            feedback.setScoreDepth(parseScoreFromText(feedbackText, "Relevance:") / 10.0);
            feedback.setScoreClarity(parseScoreFromText(feedbackText, "Clarity:") / 10.0);
            feedback.setScorePracticality(0.7); // Default value
            
            // If final score not set, calculate from individual scores
            if (feedback.getScoreFinal() == null || feedback.getScoreFinal() == 0.0) {
                double avg = (feedback.getScoreCorrectness() + feedback.getScoreCoverage() + 
                             feedback.getScoreDepth() + feedback.getScoreClarity()) / 4.0;
                feedback.setScoreFinal(avg);
            }
            
        } catch (Exception e) {
            log.warn("Error parsing scores from feedback, using defaults: {}", e.getMessage());
            setDefaultScores(feedback);
        }
    }
    
    private String extractScore(String text, String label) {
        int idx = text.indexOf(label);
        if (idx == -1) return null;
        
        int start = idx + label.length();
        int end = text.indexOf("/", start);
        if (end == -1) end = text.indexOf("\n", start);
        if (end == -1) return null;
        
        return text.substring(start, end).trim();
    }
    
    private double parseScoreFromText(String text, String label) {
        try {
            String scoreStr = extractScore(text, label);
            if (scoreStr != null) {
                return Double.parseDouble(scoreStr);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 7.0; // Default score
    }
    
    private void setDefaultScores(AnswerFeedback feedback) {
        feedback.setScoreCorrectness(0.7);
        feedback.setScoreCoverage(0.7);
        feedback.setScoreDepth(0.7);
        feedback.setScoreClarity(0.7);
        feedback.setScorePracticality(0.7);
        feedback.setScoreFinal(0.7);
    }
}

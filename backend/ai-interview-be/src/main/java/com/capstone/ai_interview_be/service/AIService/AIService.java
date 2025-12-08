package com.capstone.ai_interview_be.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskEvaluateResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskGenerateResponse;
import com.capstone.ai_interview_be.dto.response.MultitaskReportResponse;
import com.capstone.ai_interview_be.dto.response.OverallFeedbackData;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    
    private final MultitaskJudgeService multitaskJudgeService;  // NEW: Multitask v2
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate first interview question using Multitask GENERATE_FIRST or Gemini fallback
     */
    public String generateFirstQuestion(String role, String level, List<String> skills, 
                                       String cvText, String jdText) {
        // Try Multitask GENERATE_FIRST first
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 GENERATE_FIRST for role: {}", role);
            try {
                MultitaskGenerateResponse response = multitaskJudgeService.generateFirstQuestion(
                        role,
                        skills,
                        level,
                        "English",
                        cvText,
                        jdText,
                        0.7
                );
                
                if (response != null && response.getQuestion() != null && !response.getQuestion().isEmpty()) {
                    log.info("Multitask GENERATE_FIRST success - Type: {}", response.getQuestionType());
                    return response.getQuestion();
                }
            } catch (Exception e) {
                log.error("Multitask GENERATE_FIRST failed: {}", e.getMessage());
            }
        }
        
        // Fallback to Gemini
        log.warn("Multitask unavailable, using Gemini fallback for first question");
        try {
            return geminiService.generateFirstQuestion(role, skills, "English", level);
        } catch (Exception e) {
            log.error("Gemini failed: {}", e.getMessage());
            return "Please tell me a little bit about yourself and your background.";
        }
    }
    
    public String generateFirstQuestion(String role, String level, List<String> skills) {
        return generateFirstQuestion(role, level, skills, null, null);
    }
    
    /**
     * Generate next question using Multitask GENERATE (v2) or Gemini fallback
     */
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage, String sessionLevel, 
                                     String previousQuestion, String previousAnswer, String cvText, String jdText,
                                     List<ConversationEntry> conversationHistory) {
        
        // Try Multitask GENERATE first
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 GENERATE for next question");
            try {
                // Convert conversation history to List<Map>
                List<Map<String, String>> historyForAI = null;
                if (conversationHistory != null && !conversationHistory.isEmpty()) {
                    historyForAI = conversationHistory.stream()
                        .map(entry -> {
                            Map<String, String> qa = new HashMap<>();
                            qa.put("question", entry.getQuestionContent());
                            qa.put("answer", entry.getAnswerContent());
                            return qa;
                        })
                        .collect(Collectors.toList());
                }
                
                // Determine difficulty based on level
                String difficulty = "medium";
                if (sessionLevel != null) {
                    if (sessionLevel.toLowerCase().contains("junior")) {
                        difficulty = "easy";
                    } else if (sessionLevel.toLowerCase().contains("senior")) {
                        difficulty = "hard";
                    }
                }
                
                MultitaskGenerateResponse response = multitaskJudgeService.generateFollowUp(
                        previousQuestion,
                        previousAnswer,
                        historyForAI,
                        sessionRole,  // jobDomain
                        difficulty,
                        0.7
                );
                
                if (response != null && response.getQuestion() != null && !response.getQuestion().isEmpty()) {
                    log.info("Multitask GENERATE success - Type: {}", response.getQuestionType());
                    return response.getQuestion();
                }
            } catch (Exception e) {
                log.error("Multitask GENERATE failed: {}", e.getMessage());
            }
        }
        
        // Fallback to Gemini
        log.warn("Multitask unavailable, using Gemini fallback");
        try {
            return geminiService.generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                                                        previousQuestion, previousAnswer);
        } catch (Exception e) {
            log.error("Next question error: {}", e.getMessage());
            return "Can you tell me about a challenging project you've worked on recently?";
        }
    }
    
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage, String sessionLevel, 
                                     String previousQuestion, String previousAnswer) {
        return generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel, 
                                   previousQuestion, previousAnswer, null, null, null);
    }
    
    public DataScanResponse extractData(String Text) {
        try {
            String jsonResponse = geminiService.generateData(Text);
            
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty Gemini response");
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            
            if (jsonResponse.contains("Sorry") || jsonResponse.contains("error")) {
                log.error("Gemini error: {}", jsonResponse);
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            
            String cleanedJson = cleanJsonResponse(jsonResponse);
            return objectMapper.readValue(cleanedJson, DataScanResponse.class);
            
        } catch (Exception e) {
            log.error("Extract data error: {}", e.getMessage());
            return new DataScanResponse("null", "null", Arrays.asList(), "en");
        }
    }

    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null) return "{}";
        
        String cleaned = jsonResponse
            .replaceAll("```json\\s*", "")
            .replaceAll("```\\s*", "")
            .trim();
        
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}"); 
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {
        // Try Multitask EVALUATE first (v2)
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 EVALUATE for answer feedback");
            try {
                MultitaskEvaluateResponse response = multitaskJudgeService.evaluateAnswer(
                        question,
                        answer,
                        null,  // context
                        role,  // jobDomain
                        0.3
                );
                
                if (response != null) {
                    // Convert Multitask scores (0-10) to normalized scores (0-1)
                    double normalizedScore = response.getOverall() / 10.0;
                    
                    // Only show feedback text, scores are displayed separately in UI
                    String feedback = response.getFeedback() != null && !response.getFeedback().isEmpty()
                            ? response.getFeedback()
                            : "No detailed feedback available.";
                    
                    // Use improved_answer from AI if available, otherwise leave null (don't show)
                    String sampleAnswer = response.getImprovedAnswer() != null && !response.getImprovedAnswer().isEmpty()
                            ? response.getImprovedAnswer()
                            : null;  // Don't show default text, let frontend hide the section
                    
                    return AnswerFeedbackData.builder()
                            .feedback(feedback)
                            .sampleAnswer(sampleAnswer)
                            .build();
                }
            } catch (Exception e) {
                log.error("Multitask EVALUATE failed: {}", e.getMessage());
            }
        }
        
        // Fallback to Gemini
        log.warn("Multitask unavailable, using Gemini for answer feedback");
        try {
            return geminiService.generateAnswerFeedback(question, answer, role, level);
        } catch (Exception e) {
            log.error("Answer feedback error: {}", e.getMessage());
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .build();
        }
    }

    /**
     * Generate overall feedback using Multitask REPORT (v2) or Gemini fallback
     */
    public OverallFeedbackData generateOverallFeedback(
            List<ConversationEntry> conversation,
            String role,
            String level,
            List<String> skills) {
        log.info("Generating overall feedback for {} questions using Multitask v2", conversation.size());
        
        if (multitaskJudgeService.isServiceHealthy()) {
            log.info("Using Multitask Judge v2 REPORT (PRIMARY)");
            try {
                // Convert conversation to List<Map<String, String>>
                List<Map<String, String>> historyForAI = conversation.stream()
                        .map(entry -> {
                            Map<String, String> qa = new HashMap<>();
                            qa.put("question", entry.getQuestionContent());
                            qa.put("answer", entry.getAnswerContent());
                            return qa;
                        })
                        .collect(Collectors.toList());
                
                // Build candidate info
                String candidateInfo = String.format("Role: %s, Level: %s, Skills: %s",
                        role != null ? role : "Developer",
                        level != null ? level : "Mid-level",
                        skills != null ? String.join(", ", skills) : "");
                
                MultitaskReportResponse response = multitaskJudgeService.generateReport(
                        historyForAI,
                        role,  // jobDomain
                        candidateInfo,
                        0.5
                );
                
                if (response != null && response.getOverallAssessment() != null) {
                    // Convert score (0-100) to overview rating
                    String overview = convertScoreToOverview(response.getScore());
                    
                    // Convert recommendations list to string
                    String recommendations = response.getRecommendations() != null && !response.getRecommendations().isEmpty()
                            ? String.join(" ", response.getRecommendations())
                            : "Continue practicing and improving your technical interview skills.";
                    
                    log.info("Multitask REPORT success - Score: {}/100, Overview: {}", 
                            response.getScore(), overview);
                    
                    return OverallFeedbackData.builder()
                            .overview(overview)
                            .assessment(response.getOverallAssessment())
                            .strengths(response.getStrengths() != null ? response.getStrengths() : Arrays.asList())
                            .weaknesses(response.getWeaknesses() != null ? response.getWeaknesses() : Arrays.asList())
                            .recommendations(recommendations)
                            .build();
                }
                
                log.warn("Multitask REPORT invalid response, falling back");
            } catch (Exception e) {
                log.error("Multitask REPORT error, falling back: {}", e.getMessage());
            }
        } else {
            log.warn("Multitask Judge v2 unavailable, using Gemini (BACKUP)");
        }
        
        // Fallback to Gemini
        log.info("Using Gemini fallback for overall feedback");
        try {
            return geminiService.generateOverallFeedback(conversation, role, level, skills);
        } catch (Exception e) {
            log.error("Gemini failed, using hardcoded fallback: {}", e.getMessage());
            return OverallFeedbackData.builder()
                    .overview("AVERAGE")
                    .assessment("Thank you for completing the interview. Your performance showed potential. "
                               + "Due to technical difficulties, we could not generate detailed automated feedback. "
                               + "A human reviewer will evaluate your responses shortly.")
                    .strengths(Arrays.asList(
                        "Participated in the complete interview session",
                        "Attempted to answer all questions",
                        "Maintained professional communication"
                    ))
                    .weaknesses(Arrays.asList(
                        "Detailed automated evaluation unavailable",
                        "Manual review required for comprehensive feedback"
                    ))
                    .recommendations("Continue practicing technical interview questions and focus on providing detailed, structured answers. "
                                   + "A human reviewer will provide more specific feedback based on your responses.")
                    .build();
        }
    }
    
    /**
     * Convert numeric score (0-100) to overview rating
     */
    private String convertScoreToOverview(Integer score) {
        if (score == null) return "AVERAGE";
        if (score >= 85) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 50) return "AVERAGE";
        if (score >= 30) return "BELOW AVERAGE";
        return "POOR";
    }
}
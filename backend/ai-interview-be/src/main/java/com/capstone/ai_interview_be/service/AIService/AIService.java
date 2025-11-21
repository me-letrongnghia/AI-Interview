package com.capstone.ai_interview_be.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.dto.response.AnswerFeedbackData;
import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.capstone.ai_interview_be.dto.response.OverallFeedbackData;
import com.capstone.ai_interview_be.model.ConversationEntry;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    
    private final GenQService genQService;
    private final JudgeService judgeService;
    private final JudgeOverallFeedbackService judgeOverallFeedbackService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String generateFirstQuestion(String role, String level, List<String> skills, 
                                       String cvText, String jdText) {
        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ for first question");
            return genQService.generateFirstQuestion(role, level, skills, cvText, jdText);
        }
        
        log.warn("GenQ unavailable, using Gemini fallback");
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
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage, String sessionLevel, 
                                     String previousQuestion, String previousAnswer, String cvText, String jdText,
                                     List<ConversationEntry> conversationHistory) {
        List<Map<String, String>> historyForAI = null;
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            historyForAI = conversationHistory.stream()
                .map(entry -> {
                    Map<String, String> qa = new java.util.HashMap<>();
                    qa.put("question", entry.getQuestionContent());
                    qa.put("answer", entry.getAnswerContent());
                    return qa;
                })
                .collect(java.util.stream.Collectors.toList());
        }

        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ for next question");
            return genQService.generateNextQuestion(sessionRole, sessionLevel, sessionSkill, 
                                                   previousQuestion, previousAnswer, cvText, jdText, historyForAI);
        }
        
        log.warn("GenQ unavailable, using Gemini fallback");
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

    public OverallFeedbackData generateOverallFeedback(
            List<ConversationEntry> conversation,
            String role,
            String level,
            List<String> skills) {
        log.info("Generating overall feedback for {} questions", conversation.size());
        
        if (judgeOverallFeedbackService.isServiceHealthy()) {
            log.info("Using Judge AI (PRIMARY)");
            try {
                var judgeResponse = judgeOverallFeedbackService.evaluateOverallFeedback(
                        conversation, role, level, skills);
                
                if (judgeResponse != null && judgeResponse.getOverview() != null) {
                    return OverallFeedbackData.builder()
                            .overview(judgeResponse.getOverview())
                            .assessment(judgeResponse.getAssessment())
                            .strengths(judgeResponse.getStrengths())
                            .weaknesses(judgeResponse.getWeaknesses())
                            .recommendations(judgeResponse.getRecommendations())
                            .build();
                }
                
                log.warn("Judge AI invalid response, falling back");
            } catch (Exception e) {
                log.error("Judge AI error, falling back: {}", e.getMessage());
            }
        } else {
            log.warn("Judge AI unavailable, using Gemini (BACKUP)");
        }
        
        log.info("Using Gemini fallback");
        try {
            return geminiService.generateOverallFeedback(conversation, role, level, skills);
        } catch (Exception e) {
            log.error("Gemini failed, using hardcoded fallback: {}", e.getMessage());
            return OverallFeedbackData.builder()
                    .overview("AVERAGE")
                    .assessment("Thank you for completing the interview. Your performance showed potential. "
                               + "Due to technical difficulties, we could not generate detailed automated feedback. "
                               + "A human reviewer will evaluate your responses shortly.")
                    .strengths(java.util.Arrays.asList(
                        "Participated in the complete interview session",
                        "Attempted to answer all questions",
                        "Maintained professional communication"
                    ))
                    .weaknesses(java.util.Arrays.asList(
                        "Detailed automated evaluation unavailable",
                        "Manual review required for comprehensive feedback"
                    ))
                    .recommendations("Continue practicing technical interview questions and focus on providing detailed, structured answers. "
                                   + "A human reviewer will provide more specific feedback based on your responses.")
                    .build();
        }
    }
}
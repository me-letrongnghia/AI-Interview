package com.capstone.ai_interview_be.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
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
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Phương thức để tạo câu hỏi phỏng vấn đầu tiên với CV và JD text
    public String generateFirstQuestion(String role, String level, List<String> skills, 
                                       String cvText, String jdText) {
        log.info("Generating first question");   
        // Thử GenQ service trước
        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ service for first question generation");
            return genQService.generateFirstQuestion(role, level, skills, cvText, jdText);
        }
        // Fallback về Gemini
        log.warn("GenQ service unavailable, falling back to Gemini for first question");
        try {
            return geminiService.generateFirstQuestion(role, skills, "English", level);
        } catch (Exception e) {
            log.error("Error generating first question with Gemini AI, using fallback", e);
            return "Please tell me a little bit about yourself and your background.";
        }
    }
    
    // Phương thức để tạo câu hỏi phỏng vấn đầu tiên
    public String generateFirstQuestion(String role, String level, List<String> skills) {
        return generateFirstQuestion(role, level, skills, null, null);
    }
     
    // Phương thức để tạo câu hỏi tiếp theo với CV và JD text
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage, String sessionLevel, 
                                     String previousQuestion, String previousAnswer, String cvText, String jdText) {
        log.info("Generating next question");

        // Thử GenQ service trước
        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ service for next question generation");
            return genQService.generateNextQuestion(sessionRole, sessionLevel, sessionSkill, 
                                                   previousQuestion, previousAnswer, cvText, jdText);
        }
        // Fallback về Gemini
        log.warn("GenQ service unavailable, falling back to Gemini for next question");
        try {
            return geminiService.generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                                                        previousQuestion, previousAnswer);
        } catch (Exception e) {
            log.error("Error generating next question", e);
            return "Can you tell me about a challenging project you've worked on recently?";
        }
    }
    
    // Phương thức để tạo câu hỏi tiếp theo
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage, String sessionLevel, 
                                     String previousQuestion, String previousAnswer) {
        return generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel, 
                                   previousQuestion, previousAnswer, null, null);
    }
    
    // Phương thức để trích xuất dữ liệu từ CV, JD
    public DataScanResponse extractData(String Text) {
        try {
            log.info("Extracting data from CV, JD text: {}", Text != null ? Text.substring(0, Math.min(100, Text.length())) : "null");
            String jsonResponse = geminiService.generateData(Text);

            log.info("Raw JSON response from Gemini: {}", jsonResponse);
            // Kiểm tra response rỗng
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty response from Gemini service");
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            
            // Kiểm tra lỗi trong response
            if (jsonResponse.contains("Sorry") || jsonResponse.contains("error")) {
                log.error("Gemini service returned error message: {}", jsonResponse);
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            
            // Clean JSON response nếu cần
            String cleanedJson = cleanJsonResponse(jsonResponse);
            log.info("Cleaned JSON: {}", cleanedJson);
            
            // Parse JSON thành DataScanResponse
            DataScanResponse response = objectMapper.readValue(cleanedJson, DataScanResponse.class);
            log.info("Successfully parsed DataScanResponse: {}", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error parsing CV data extraction response: {}", e.getMessage());
            return new DataScanResponse("null", "null", Arrays.asList(), "en");
        }
    }

    // Hàm để làm sạch JSON response nếu có ký tự thừa
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null) return "{}";
        
        // Remove markdown code fences (```json, ```, etc.)
        String cleaned = jsonResponse
            .replaceAll("```json\\s*", "")  // Remove ```json
            .replaceAll("```\\s*", "")       // Remove trailing ```
            .trim();
        
        // Extract JSON object
        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}"); 
        if (start != -1 && end != -1 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    // Generate feedback cho một câu trả lời
    // Ưu tiên GenQ service, fallback về Gemini nếu không khả dụng
    public AnswerFeedbackData generateAnswerFeedback(String question, String answer, String role, String level) {
        log.info("Generating answer feedback for question: {}", question);
        
        // Hiện tại chỉ dùng Gemini vì GenQ chưa được train cho feedback
        // Sau này có thể thêm GenQ service khi model đã được train
        try {
            return geminiService.generateAnswerFeedback(question, answer, role, level);
        } catch (Exception e) {
            log.error("Error generating answer feedback with Gemini, using fallback", e);
            return AnswerFeedbackData.builder()
                    .feedback("Unable to generate detailed feedback at this moment.")
                    .sampleAnswer("Please review the question and try to provide more specific details.")
                    .build();
        }
    }

    // Generate overall feedback cho cả session
    // Ưu tiên GenQ service, fallback về Gemini nếu không khả dụng     
    public OverallFeedbackData generateOverallFeedback(
            List<ConversationEntry> conversation,
            String role,
            String level,
            List<String> skills) {
        log.info("Generating overall feedback for session with {} questions", conversation.size());
        
        // Hiện tại chỉ dùng Gemini vì GenQ chưa được train cho feedback
        // Sau này có thể thêm GenQ service khi model đã được train
        try {
            return geminiService.generateOverallFeedback(conversation, role, level, skills);
        } catch (Exception e) {
            log.error("Error generating overall feedback with Gemini, using fallback", e);
            return OverallFeedbackData.builder()
                    .overview("Hiiiiiiiiii")
                    .assessment("Thank you for completing the interview. Your performance showed potential.")
                    .strengths(java.util.Arrays.asList(
                        "Participated in the interview",
                        "Attempted to answer questions"
                    ))
                    .weaknesses(java.util.Arrays.asList(
                        "Could provide more detailed responses"
                    ))
                    .recommendations("Continue practicing technical interview questions and focus on providing detailed, structured answers.")
                    .build();
        }
    }
}
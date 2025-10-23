package com.capstone.ai_interview_be.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.capstone.ai_interview_be.dto.response.DataScanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {
    
    private final GenQService genQService;
    private final OpenRouterService openRouterService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Tạo câu hỏi phỏng vấn đầu tiên
     * Ưu tiên GenQ service, fallback về OpenRouter nếu không khả dụng
     */
    public String generateFirstQuestion(String role, String level, List<String> skills) {
        log.info("Generating first question for role: {}, level: {}, skills: {}", role, level, skills);
        
        // Thử GenQ service trước
        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ service for first question generation");
            return genQService.generateFirstQuestion(role, level, skills);
        }
        
        // Fallback về OpenRouter
        log.warn("GenQ service unavailable, falling back to OpenRouter for first question");
        try {
            return openRouterService.generateFirstQuestion(role, skills, "English", level);
        } catch (Exception e) {
            log.error("Error generating first question with OpenRouter AI, using fallback", e);
            return "Please tell me a little bit about yourself and your background.";
        }
    }
     
    /**
     * Tạo câu hỏi tiếp theo dựa trên câu hỏi và trả lời trước đó
     * Ưu tiên GenQ service, fallback về OpenRouter nếu không khả dụng
     */
    public String generateNextQuestion(String sessionRole, List<String> sessionSkill, String sessionLanguage, String sessionLevel, 
                                     String previousQuestion, String previousAnswer) {
        log.info("Generating next question for role: {}, skill: {}, language: {}, level: {}", 
                sessionRole, sessionSkill, sessionLanguage, sessionLevel);
        
        // Thử GenQ service trước
        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ service for next question generation");
            return genQService.generateNextQuestion(sessionRole, sessionLevel, sessionSkill, 
                                                   previousQuestion, previousAnswer);
        }
        
        // Fallback về OpenRouter
        log.warn("GenQ service unavailable, falling back to OpenRouter for next question");
        try {
            return openRouterService.generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
                                                        previousQuestion, previousAnswer);
        } catch (Exception e) {
            log.error("Error generating next question with OpenRouter AI, falling back to mock", e);
            return "Can you tell me about a challenging project you've worked on recently?";
        }
    }
    

    // Phương thức để trích xuất dữ liệu từ CV, JD
    public DataScanResponse extractData(String cvText) {
        try {
            log.info("Extracting data from CV text: {}", cvText != null ? cvText.substring(0, Math.min(100, cvText.length())) : "null");

            String jsonResponse = openRouterService.generateData(cvText);
            
            log.info("Raw JSON response from OpenRouter: {}", jsonResponse);
            
            // Validate JSON response trước khi parse
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty response from OpenRouter service");
                return new DataScanResponse("Software Engineer", "Fresher", Arrays.asList(), "English");
            }
            
            // Clean JSON response (loại bỏ text thừa nếu có)
            String cleanedJson = cleanJsonResponse(jsonResponse);
            log.info("Cleaned JSON: {}", cleanedJson);
            
            DataScanResponse response = objectMapper.readValue(cleanedJson, DataScanResponse.class);
            log.info("Successfully parsed DataScanResponse: {}", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error parsing CV data extraction response. Raw response: {}", 
                     openRouterService.generateData(cvText), e);
            
            // Trả về default có ý nghĩa hơn
            return new DataScanResponse("Software Engineer", "Fresher", Arrays.asList(), "English");
        }
    }

    // Helper method để clean JSON response
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse == null) return "{}";
        
        // Tìm JSON object trong response
        int start = jsonResponse.indexOf("{");
        int end = jsonResponse.lastIndexOf("}");
        
        if (start != -1 && end != -1 && end > start) {
            return jsonResponse.substring(start, end + 1);
        }
        
        return jsonResponse.trim();
    }


    
}
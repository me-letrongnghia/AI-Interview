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
    
    // Phương thức để tạo câu hỏi phỏng vấn đầu tiên với CV và JD text
    public String generateFirstQuestion(String role, String level, List<String> skills, 
                                       String cvText, String jdText) {
        log.info("Generating first question");   
        // Thử GenQ service trước
        if (genQService.isServiceHealthy()) {
            log.info("Using GenQ service for first question generation");
            return genQService.generateFirstQuestion(role, level, skills, cvText, jdText);
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
        // Fallback về OpenRouter
        log.warn("GenQ service unavailable, falling back to OpenRouter for next question");
        try {
            return openRouterService.generateNextQuestion(sessionRole, sessionSkill, sessionLanguage, sessionLevel,
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
            String jsonResponse = openRouterService.generateData(Text);

            log.info("Raw JSON response from OpenRouter: {}", jsonResponse);
            // Kiểm tra response rỗng
            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                log.warn("Empty response from OpenRouter service");
                return new DataScanResponse("null", "null", Arrays.asList(), "en");
            }
            
            // Kiểm tra lỗi trong response
            if (jsonResponse.contains("Sorry") || jsonResponse.contains("error")) {
                log.error("OpenRouter service returned error message: {}", jsonResponse);
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
        int start = jsonResponse.indexOf("{");
        int end = jsonResponse.lastIndexOf("}"); 
        if (start != -1 && end != -1 && end > start) {
            return jsonResponse.substring(start, end + 1);
        }
        return jsonResponse.trim();
    }
  
}